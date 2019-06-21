# Melanie
Melanie is an online ensemble method enable to cope with concept drift with multi-sources. 

Melanie is the first approach able to transfer knowledge between multiple data streaming sources in non-stationary environments. 

The full name is Multi-sourcE onLine TrAnsfer learning for Non-statIonary Environments. 

It has been accepted by IJCNN 2019, we will update the reference after the conference.

There is also an ArXiv version available now:
Du, H., Minku, L.L. and Zhou, H., 2019. Multi-Source Transfer Learning for Non-Stationary Environments. arXiv preprint arXiv:1901.02052. 
https://arxiv.org/pdf/1901.02052.pdf

## Dependencies
MOA-release-2018.6.0 <<<It may work with other versions, but we haven't tested with them>>>.

poi-3.17 <<<It may work with other versions, but we haven't tested with them>>>.

## DataSets
I put all the datasets used in the experiments into repository.
 
For each experiment, we put different sources data into one arff.file. Each problem instance has three types associated data sets as follows format:

(name of data set)_(class size/Percentage of target)_(source size)_(m/s).arff

(name of data set) This is the name of data set.

(class size/Percentage of target) For artifical data sets, this is the class size of target data. 
For real world data sets, this is the percentage of source data we extracted from origenal data sets.

(source size) this is to indicate how many sources are used in the data set.

(m/s) m means this data set can be used for Melanie. s means this data set can be used for other algorithms.

The data set Melanie used will have one additional feature before other features to indicate where the source comes from.
For example, 1 means the instance comes from source 1 and 0 means the instance comes from the target domain. 

The three different types files contain target training examples in the same chronological order.

## Running Models
Please copy and paste all the code into MOA's src folder.

There are two ways to run the models.

First is using MOA's GUI.

Seconde is runing from command line by using MOA's command format.
For example, java -Xmx$MEMORY -cp "$BASEDIR/lib/moa-2018.6.0:$BASEDIR/lib/*" -javaagent:$BASEDIR/lib/sizeofag-1.0.4.jar moa.DoTask "EvaluatePrequential -l (meta.Melanie -b (meta.OzaBag -e ensemble_size -r random_seed) -t theta_value -l lamda_value -a threshhold -i dataset_size -c concept_drift_position -u $i -f (output_path)) -s (ArffFileStream -f dataset_path)"

