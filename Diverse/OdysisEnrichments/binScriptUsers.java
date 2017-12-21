/*************************************************************************
 * ULLINK CONFIDENTIAL INFORMATION
 * _______________________________
 *
 * All Rights Reserved.
 *
 * NOTICE: This file and its content are the property of Ullink. The
 * information included has been classified as Confidential and may
 * not be copied, modified, distributed, or otherwise disseminated, in
 * whole or part, without the express written permission of Ullink.
 ************************************************************************/
#######
# Override given user id's password with given password
#
#   - $1: username
#   - $2: new password
#
function jmx_select_user() {
    local userid=$1;
    
    go bin
    
    ./jmx-client.sh com.ullink.ultools.dao.core:type=user select='"'$1'".equals(record.getId())'
    
}

#######
# Override given user id's password with given password
#
#   - $1: username
#   - $2: new password
#
function jmx_reset_user_password() {
    local userid=$1;
    local newpassword=$2;
    
    go bin
    
    ./jmx-client-extended.sh com.ullink.ultools.dao.core:type=user update='"'$1'".equals(record.getId())','record.setPassword("'$2'")'
    
}

    function ods_create_default_users() {
        go bin
        [[ -f jmx-client-extended.sh ]] && cp /home/ullink/COMMON/SCRIPTS/jmx-client-extended.sh .
        
        # need to copy file to tmp directory due to some issues with MSYS and ODS pre 3.2
        [[ -d ../logs/tmp/ ]] && mkdir -p ../logs/tmp/
        cp $INSTALL_PATH/ulscripts/usercreation/UserCreation.java ../logs/tmp/
        javaSource="../logs/tmp/UserCreation.java"
        
        set -x
         ./jmx-client-extended.sh -action='com.ullink.oms.jmx.mbeans:type=EnrichmentManagement,name=EnrichmentManagement.compile("action", "UserCreation", file='${javaSource}')'
        set +x
        
        rm $javaSource
    }

#######
# Override given user id's password with given password, using a java enrichment
#
#   - $1: username
#   - $2: new password
#
function ods_update_user() {
    local userid=$1;
    local newpassword=$2;
    
    go bin
    
    [[ -d ../logs/tmp/ ]] && mkdir -p ../logs/tmp/
    
    local tempfile="../logs/tmp/"$(cat /dev/urandom | tr -dc 'a-zA-Z' | fold -w 8 | head -n 1).java # we don't use mktemp as it generates ids with numbers, which are illegal for java class names
    local classname=$(basename ${tempfile%.*}) # get rid of .java extension
    
    template "$INSTALL_PATH/ulscripts/usercreation/UserUpdate.java"  username=$1 password=$2 classname=$classname > $tempfile
    
    [[ -f jmx-client-extended.sh ]] && cp /home/ullink/COMMON/SCRIPTS/jmx-client-extended.sh .
    
     ./jmx-client-extended.sh -action='com.ullink.oms.jmx.mbeans:type=EnrichmentManagement,name=EnrichmentManagement.compile("action", "'${classname}'", file='$tempfile')'
     rm $tempfile

}

#####
# Utility templating function allowing to execute java code based on a template
# I used the reply to this SO question:
# http://stackoverflow.com/questions/9961262/how-to-include-a-template-txt-as-a-here-doc-of-a-script
#
function template() {
  file=$1
  shift
  eval "`printf 'local %s\n' $@`
cat <<EOF
`cat $file`
EOF"
}
# Keep this last
echo ">> [$BASH_SOURCE] loaded"