# Testing

This document contains information on test configurations, stored in the `configs` directory.
These configurations are discovered during file indexing and are ready to use without any further configuration.
Each test configuration runs all tests present in the source directory and stores results in the results directory, as
shown in the table below. This document assumes that the testing machine is running Windows, and that the
[ :book: ] [`DEVELOPING`](./DEVELOPING.md) guide has already been completed.

### Configurations

|              Name               |                              Source                               |                    Results                     |
|:-------------------------------:|:-----------------------------------------------------------------:|:----------------------------------------------:|
|      [ :gear: ] `AllTests`      |              [ :open_file_folder: ] `src/test/java`               |                       -                        |
|      [ :gear: ] `E2ETests`      |      [ :open_file_folder: ] `src/test/java/org/ascent/e2es`       | [ :open_file_folder: ] `src/test/results/e2es` |
| [ :gear: ] `FunctionalityTests` | [ :open_file_folder: ] `src/test/java/org/ascent/functionalities` |                       -                        |
|  [ :gear: ] `IntegrationTests`  |  [ :open_file_folder: ] `src/test/java/org/ascent/integrations`   |                       -                        |
|     [ :gear: ] `UnitTests`      |      [ :open_file_folder: ] `src/test/java/org/ascent/units`      |                       -                        |