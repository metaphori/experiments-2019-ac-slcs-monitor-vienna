# Adaptive distributed monitors of spatial properties for cyber-physical systems

This is the project repository for the case study
 presented in a manuscript submitted to a journal.

## Structure of the repository

The project has the standard structure of Maven/Gradle projects.

* `./build.gradle` contains the build definition and the tasks for compiling and packaging the application
* `./src/main/protelis/monitoringAndDispersal.pt` contains the **source code of the case study**
* `./src/main/yaml/monitoringAndDispersalWithAPs.yml` contains the **configuration of the Alchemist simulation**
* `./plotter.py` is a script that can be used to plot data produced by Alchemist, according to a configuration file such as `./plot.yml`

## Reproducing the experiments: how-to

NOTE: this may take a few hours.

The simulations used to produce the results shown in the paper can be run with the following command:

```bash
$ git checkout paper-experiments
$ ./gradlew paper-experiments
```

Once data files (which are in CSV format) are generated in directory `data/`, you can use script `plotter.py` to create plots.

```bash
# ./plotter.py <dir> <baseFilepath> <plotConfigFile>
$ ./plotter.py data simulations plot.yml
```

## Pictures in the paper



## Contacts

* Roberto Casadei: roby [dot] casadei [at] unibo [dot] it
