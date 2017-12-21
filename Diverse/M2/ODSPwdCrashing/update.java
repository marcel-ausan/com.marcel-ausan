/*
 * Dear all,

As you may or may not know, there's an issue related to Odisys upgrade, where some users will get their passwords "corrupted" as part of the passwords re-hashing mechanism when you upgrade to Odisys 4.0+, which is the bare minimum we need for Mifid 2 resilience.

Product team provided a script as part of ticket OMSX-18159 in order to allow us to fix this issue.

In order to run the attached scripts, please make sure you use jmx-client-extended.sh:
cd /mnt/xxodisys/bin 
./jmx-client-extended.sh '-action=com.ullink.ultools.dao.core:type=user.update(file=select.java,file=update.java)'

As far as I know, you must run the script before the upgrade.
 */


for (com.ullink.oms.model.Password password : record.getPasswordSaved())
{
    if (password.getValue().contains("\\="))
    {
        password.setValue(password.getValue().replaceAll("\\\\=", "="));
    }
}