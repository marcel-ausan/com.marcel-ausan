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

public class SQLReplace
{

}

BEGIN

CURSOR passwordsToBeCorrupted
IS
    SELECT passwordColumn 
    FROM userTable
    WHERE passwordColumn LIKE ('%\\=%');

FOR cur_index IN passwordsToBeCorrupted
LOOP

    UPDATE userTable
    SET passwordColumn = REPLACE(passwordColumn, '\\=', '=')
    WHERE passwordColumn LIKE ('%\\=%');

END LOOP;

END
