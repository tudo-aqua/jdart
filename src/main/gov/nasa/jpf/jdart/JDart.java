/*
 * Copyright (C) 2015, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jpf.jdart;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFShell;
import gov.nasa.jpf.jdart.config.ConcolicConfig;
import gov.nasa.jpf.jdart.config.ConcolicMethodConfig;
import gov.nasa.jpf.jdart.constraints.tree.ConstraintsTreeAnalysis;
import gov.nasa.jpf.jdart.testsuites.TestSuiteGenerator;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.LogManager;
import gov.nasa.jpf.util.SimpleProfiler;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/** The actual jdart jpf-shell to be started from config files. */
public class JDart implements JPFShell {

  public static final String CONFIG_KEY_CONCOLIC_EXPLORER = "jdart.concolic_explorer_instance";

  private final Config config;
  private final ConcolicConfig cc;

  private JPFLogger logger = null; // JPF.getLogger("jdart")

  // LEGACY API
  private ConcolicExplorer explorer;

  /**
   * Constructor. Initializes JDart from a JPF config only.
   *
   * @param conf the JPF config
   */
  public JDart(Config conf) {
    // this(conf, true);
    // due to some bug the log manager has to be initialized first.
    LogManager.init(conf);
    this.config = conf;
    this.cc = new ConcolicConfig(conf);
    logger = JPF.getLogger("jdart");
  }

  /**
   * Constructor. Initializes JDart from a JPF config only. Allows to control whether the logging
   * system should be initialized as well.
   *
   * @param conf the JPF config
   * @param initLogging whether or not to initialize the logging system.
   */
  public JDart(Config conf, boolean initLogging) {
    this(conf, new ConcolicConfig(conf), initLogging);
  }

  /**
   * Constructor. Initializes JDart with a given JPF config and concolic configuration.
   *
   * @param conf the JPF config
   * @param cc the concolic configuration
   */
  public JDart(Config conf, ConcolicConfig cc) {
    this(conf, cc, false);
  }

  /**
   * Constructor. Initializes JDart with a given concolic configuration.
   *
   * @param cc the concolic configuration
   */
  public JDart(ConcolicConfig cc) {
    this(null, cc, false);
  }

  /**
   * Constructor. Initializes JDart from a JPF config and a concolic configuration. Allows whether
   * the logging system should be initialized as well.
   *
   * @param conf the JPF config
   * @param cc the concolic configuration
   * @param init whether or not to initialize the logging system.
   */
  public JDart(Config conf, ConcolicConfig cc, boolean init) {
    this.config = conf;
    this.cc = cc;
    if (init) {
      LogManager.init(conf);
    }
    logger = JPF.getLogger("jdart");
  }

  public static ConcolicExplorer getConcolicExplorer(Config config) {
    ConcolicExplorer exp =
        config.getEssentialInstance(CONFIG_KEY_CONCOLIC_EXPLORER, ConcolicExplorer.class);
    if (!exp.isConfigured()) {
      exp.configure(new ConcolicConfig(config));
    }
    return exp;
  }

  /*
   * (non-Javadoc)
   * @see gov.nasa.jpf.JPFShell#start(java.lang.String[])
   */
  @Override
  public void start(String[] strings) {
    run();
    // FIXME: The portfoliosolver does not exit gracefully due to a threadpool.
    // But we can ensure ressources are cleaned up nicely.
    System.exit(0);
  }

  /**
   * Run the concolic execution.
   *
   * @return the {@link ConcolicExplorer} containing the analysis data.
   */
  public ConcolicExplorer run() {

    logger.finest("JDart.run() -- begin");

    // prepare config
    Config jpfConf = cc.generateJPFConfig(config);

    // Configure JPF
    jpfConf.remove("shell");
    jpfConf.setProperty("jvm.insn_factory.class", ConcolicInstructionFactory.class.getName());
    jpfConf.prepend("peer_packages", "gov.nasa.jpf.jdart.peers", ";");

    String listener = ConcolicListener.class.getName();
    if (jpfConf.hasValue("listener")) listener += ";" + jpfConf.getString("listener");
    jpfConf.setProperty("listener", listener);
    jpfConf.setProperty("perturb.class", ConcolicPerturbator.class.getName());
    jpfConf.setProperty("search.multiple_errors", "true");

    //
    jpfConf.setProperty(
        CONFIG_KEY_CONCOLIC_EXPLORER, ConcolicExplorer.class.getName() + "@jdart-explorer");
    ConcolicExplorer ce = getConcolicExplorer(jpfConf);
    this.explorer = ce;
    ce.configure(cc);

    // set up logger. Maybe this should not be done here
    // TODO: this is ugly. Clean it up
    if (!jpfConf.getProperty("jdart.log.output", "").equals("")) {
      try {
        FileHandler fh = new FileHandler(jpfConf.getProperty("jdart.log.output"));
        logger.addHandler(fh);
        fh.setFormatter(
            new Formatter() {
              @Override
              public String format(LogRecord record) {
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                sb.append(record.getLevel().getName());
                sb.append("] ");
                String msg = record.getMessage();
                Object[] params = record.getParameters();
                if (params == null) {
                  sb.append(msg);
                } else {
                  sb.append(String.format(msg, params));
                }
                sb.append('\n');
                return sb.toString();
              }
            });
      } catch (SecurityException e1) {
        e1.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }

    logger.finest("============ JPF Config     ============");
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    jpfConf.list(pw);
    pw.close();
    logger.finest(sw.toString());
    logger.finest("============ End JPF Config ============");

    // run jpf ...
    JPF jpf = new JPF(jpfConf);
    SimpleProfiler.start("JDART-run");
    SimpleProfiler.start("JPF-boot"); // is stopped upon searchStarted in ConcolicListener
    jpf.run();
    SimpleProfiler.stop("JDART-run");

    // post process ...
    logger.finest("JDart.run() -- end");
    logger.info("Profiling:\n" + SimpleProfiler.getResults());

    FileOutputStream outStream = null;
    PrintStream printStream = null;

    String concolicValuesFileName = config.getProperty("concolic.values_file");
    if (concolicValuesFileName != null) {
      try {
        outStream = new FileOutputStream(concolicValuesFileName);
        printStream = new PrintStream(outStream);
      } catch (Exception ex) {
        logger.severe(ex);
      }
    }

    if (ce.hasCurrentAnalysis()) {
      ce.completeAnalysis();
    }

    logger.info("Completed Analyses: " + ce.getCompletedAnalyses().size());
    System.err.println("Completed Analyses: " + ce.getCompletedAnalyses().size());

    for (Map.Entry<String, List<CompletedAnalysis>> e : ce.getCompletedAnalyses().entrySet()) {
      String id = e.getKey();
      ConcolicMethodConfig mc = cc.getMethodConfig(id);
      logger.info();
      logger.info("Analyses for method ", mc);
      logger.info("==================================");
      for (CompletedAnalysis ca : e.getValue()) {

        if (ca.getConstraintsTree() == null) {
          logger.info("tree is null");
          continue;
        }

        if (config.getBoolean("jdart.tests.gen")) {
          try {
            TestSuiteGenerator gen = TestSuiteGenerator.fromAnalysis(ca, config);
            gen.generate();
          } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
          }
        }

        // FIXME: refactor this.
        ConstraintsTreeAnalysis ctAnalysis = new ConstraintsTreeAnalysis(ca.getConstraintsTree());

        // logger.info("Initial valuation: ", ca.getInitialValuation());
        if (!config.getBoolean("jdart.tree.dont.print")) {
          logger.info(ctAnalysis.toString());
        }
        if (config.getBoolean("jdart.tree.json.print")) {
          // ca.getConstraintsTree().toJson(config.getProperty("jdart.tree.json.dir") +
          //  "/" + jpfConf.getProperty("jpf.app") + ".json");
        }

        logger.info("----Constraints Tree Statistics---");
        // logger.info("# paths (total): " + ctAnalysis.getAllPaths().size());
        logger.info("# OK paths: " + ctAnalysis.getOkLeafs().size());
        logger.info("# ERROR paths: " + ctAnalysis.getErrorLeafs().size());
        logger.info("# UNSAT paths: " + ctAnalysis.getUnsatLeafs().size());
        logger.info("# DONT_KNOW paths: " + ctAnalysis.getDontKnowLeafs().size());
        logger.info("# OPEN paths: " + ctAnalysis.getOpenLeafs().size());
        logger.info("# SKIPPED paths: " + ctAnalysis.getSkippedLeafs().size());
        logger.info("# BUGGY paths: " + ctAnalysis.getBuggyLeafs().size());
        logger.info("# DIVERGED paths: " + ctAnalysis.getDivergedLeafs().size());
        logger.info("");

        logger.info("-------Valuation Statistics-------");
        logger.info(
            "# of valuations (OK+ERR): "
                + (ctAnalysis.getOkLeafs().size() + ctAnalysis.getErrorLeafs().size()));
        logger.info("");
        logger.info("--------------------------------");
      }
    }

    if (printStream != null) {
      try {
        printStream.close();
      } catch (Exception ex) {
        logger.severe(ex);
      }
    }

    return ce;
  }

  @Deprecated
  public ConcolicExplorer getConcolicExplorer() {
    return explorer;
  }
}
