/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.runner.format;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.BenchResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.BenchmarkParams;
import org.openjdk.jmh.runner.IterationParams;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

/**
 * TextReportFormat implementation of OutputFormat.
 */
class TextReportFormat extends AbstractOutputFormat {

    public TextReportFormat(PrintStream out, VerboseMode verbose) {
        super(out, verbose);
    }

    @Override
    public void startBenchmark(BenchmarkParams params) {
        if (params.getWarmup().getCount() > 0) {
            out.println("# Warmup: " + params.getWarmup().getCount() + " iterations, " +
                    params.getWarmup().getTime() + " each" +
                    (params.getWarmup().getBatchSize() <= 1 ? "" : ", " + params.getWarmup().getBatchSize() + " calls per batch"));
        } else {
            out.println("# Warmup: <none>");
        }

        if (params.getMeasurement().getCount() > 0) {
            out.println("# Measurement: " + params.getMeasurement().getCount() + " iterations, " +
                    params.getMeasurement().getTime() + " each" +
                    (params.getMeasurement().getBatchSize() <= 1 ? "" : ", " + params.getMeasurement().getBatchSize() + " calls per batch"));
        } else {
            out.println("# Measurement: <none>");
        }

        out.println("# Threads: " + params.getThreads() + " " + getThreadsString(params.getThreads()) +
                (params.shouldSynchIterations() ?
                        ", will synchronize iterations" :
                        (params.getMode() == Mode.SingleShotTime) ? "" : ", ***WARNING: Synchronize iterations are disabled!***"));
        out.println("# Benchmark mode: " + params.getMode().longLabel());
        out.println("# Benchmark: " + params.getBenchmark());
        if (!params.getParams().isEmpty()) {
            out.println("# Parameters: " + params.getParams());
        }
    }

    @Override
    public void iteration(BenchmarkParams benchmarkParams, IterationParams params, int iteration, IterationType type) {
        switch (type) {
            case WARMUP:
                out.print(String.format("# Warmup Iteration %3d: ", iteration));
                break;
            case MEASUREMENT:
                out.print(String.format("Iteration %3d: ", iteration));
                break;
            default:
                throw new IllegalStateException("Unknown iteration type: " + type);
        }
        out.flush();
    }

    protected static String getThreadsString(int t) {
        if (t > 1) {
            return "threads";
        } else {
            return "thread";
        }
    }

    @Override
    public void iterationResult(BenchmarkParams benchmParams, IterationParams params, int iteration, IterationType type, IterationResult data) {
        StringBuilder sb = new StringBuilder();
        sb.append(data.getPrimaryResult().toString());

        if (type == IterationType.MEASUREMENT) {
            int prefixLen = String.format("Iteration %3d: ", iteration).length();

            Map<String, Result> secondary = data.getSecondaryResults();
            if (!secondary.isEmpty()) {
                sb.append("\n");

                int maxKeyLen = 0;
                for (Map.Entry<String, Result> res : secondary.entrySet()) {
                    maxKeyLen = Math.max(maxKeyLen, res.getKey().length());
                }

                for (Map.Entry<String, Result> res : secondary.entrySet()) {
                    sb.append(String.format("%" + prefixLen + "s", ""));
                    sb.append(String.format("  %-" + (maxKeyLen + 1) + "s %s", res.getKey() + ":", res.getValue()));
                    sb.append("\n");
                }
            }
        }

        out.print(String.format("%s%n", sb.toString()));
        out.flush();
    }

    @Override
    public void endBenchmark(BenchResult result) {
        out.println();
        if (result != null) {
            out.println(result.getPrimaryResult().extendedInfo(null));
            for (Result r : result.getSecondaryResults().values()) {
                out.println(r.extendedInfo(r.getLabel()));
            }
            out.println();
        }
    }

    @Override
    public void startRun() {
        // do nothing
    }

    @Override
    public void endRun(Collection<RunResult> runResults) {
        PrintWriter pw = new PrintWriter(out);
        ResultFormatFactory.getInstance(ResultFormatType.TEXT, pw).writeOut(runResults);
        pw.flush();
        pw.close();
    }

}
