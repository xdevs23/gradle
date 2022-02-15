#!/bin/sh

gradle-profiler --benchmark --scenario-file performance.scenarios --project-dir . CleanCompileAllBuild
gradle-profiler --benchmark --scenario-file performance.scenarios --project-dir . TestOnlyChange
gradle-profiler --benchmark --scenario-file performance.scenarios --project-dir . IncrementalBuildWithNonABIChange
gradle-profiler --benchmark --scenario-file performance.scenarios --project-dir . IncrementalBuildWithABIChange
gradle-profiler --benchmark --scenario-file performance.scenarios --project-dir . IncrementalBuildWithBuildLogicChange
