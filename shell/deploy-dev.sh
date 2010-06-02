#! /bin/bash

[ -z "$1" ] && exit -1

export PATH="$PWD/$(dirname $0):$PATH"

cd "$1" || exit -1

source deploy.inc.sh

ant clean
#ant debug
ant release < ${rpath}release.ks.pw
#adb -d install -r bin/*-debug.apk || adb -d install -r bin/*-release.apk

pversion=${pversion}-dev-$(date +%Y%m%d)

mv bin/*-release.apk bin/${fname}-${pversion}.apk

[ -n "${gproject}" ] && \
googlecode_upload.py  -u ${gcodelogin} -w ${gcodepassw} -p ${gproject}  -s "${sname}-${pversion}"  -l Type-Package,OpSys-Android${lextra} bin/${fname}-${pversion}.apk

