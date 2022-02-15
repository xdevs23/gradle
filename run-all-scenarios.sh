#!/bin/sh

gradle-profiler --profile buildscan --scenario-file performance.scenarios --project-dir . CleanCompileAllBuild
gradle-profiler --profile buildscan --scenario-file performance.scenarios --project-dir . TestOnlyChange
gradle-profiler --profile buildscan --scenario-file performance.scenarios --project-dir . IncrementalBuildWithNonABIChange
gradle-profiler --profile buildscan --scenario-file performance.scenarios --project-dir . IncrementalBuildWithABIChange
gradle-profiler --profile buildscan --scenario-file performance.scenarios --project-dir . IncrementalBuildWithBuildLogicChange
