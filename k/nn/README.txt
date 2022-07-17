module load openmind/singularity
cd /om2/user/mjgroth/registered/share
singularity exec -H /om2/user/mjgroth --nv kotlinDLdemo.simg /bin/bash
java -jar bin/jar/kotlinDLdemo.jar 3 # <- number of epochs