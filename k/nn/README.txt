this currently doesnt work for other users because files need to be writable

I will probably need to create a shadow jar if other users want to execute

cd /om2/user/mjgroth/kcomp
module load openmind/singularity
singularity exec -B /om2/user/mjgroth/kcomp:/om2/user/mjgroth/kcomp:ro -B /om2/user/mjgroth/data:/om2/user/mjgroth/data:ro -H /om2/user/mjgroth --nv kcomp.simg /bin/bash
cd /om2/user/mjgroth/kcomp
/opt/gradle/gradle-7.5-rc-4/bin/gradle k:nn:run --console=plain