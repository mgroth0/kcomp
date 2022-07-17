cd /om2/user/mjgroth/kcomp
module load openmind/singularity
singularity exec -B /om2/user/mjgroth/data:/om2/user/mjgroth/data:ro -H /om2/user/mjgroth --nv kcomp.simg /bin/bash
java -jar /om2/user/mjgroth/registered/bin/jar/nn.jar 3 # <- number of epochs