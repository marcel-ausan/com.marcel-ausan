package utilities;

import java.util.Map.Entry;

import com.ullink.ulbridge2.ULMessage;
import com.ullink.ultools.dao.core.api.exception.DAOUnexpectedException;
import com.ullink.ultools.dao.core.tools.DataBuffer;
import com.ullink.ultools.dao.core.tools.DataHelper;
import com.ullink.ultools.dao.core.tools.SharedByteBuffer;

/*
 * A utility class to complement DataHelper
 */
public class ExtendedDataHelper
{
    private static final String ESCAPE_SEQUENCE        = (new StringBuilder()).append(String.valueOf('\004')).append(String.valueOf('\003')).toString();
    private static final int    ESCAPE_SEQUENCE_LENGTH = ESCAPE_SEQUENCE.length();
    private static final byte[] ESCAPE_SEQUENCE_BYTES  = ESCAPE_SEQUENCE.getBytes();

    public static ULMessage getULMessage(DataBuffer cs)
    {
        int length = DataHelper.getInteger(cs);
        if (length == 0)
        {
            return null;
        }
        String ulmStr = getULMString(cs.getBuffer(), cs.getOffset(), length);
        cs.inc(length);
        return ULMessage.valueOf(ulmStr);
    }

    private static ThreadLocal<char[]> messageBuffer = new ThreadLocal<char[]>()
                                                     {
                                                         @Override
                                                         protected char[] initialValue()
                                                         {
                                                             return new char[1024];
                                                         }
                                                     };

    private static int                 base_length   = 1024; // default length

    private static char[] getMessageArray(int length)
    {
        char[] current = messageBuffer.get();
        if (current.length < length)
        {
            current = new char[length];
            messageBuffer.set(current);
        }
        return current;
    }

    private final static String getULMString(byte bytes[], int offset, int length)
    {
        if (bytes == null)
        {
            return null;
        }
        int maxLength = bytes.length;
        if (length > (maxLength - offset))
        {
            throw new DAOUnexpectedException("expected length [" + length + "] is greater than bytes.length [" + maxLength + "] minus offset [" + offset + "]");
        }
        char[] chars = getMessageArray(length);
        for (int i = 0; i < length && offset < maxLength; i++)
        {
            chars[i] = (char) bytes[offset++];
            chars[i] &= 0xFF;
        }
        return new String(chars, 0, length);
    }

    public static void writeULMessage(SharedByteBuffer cs, ULMessage ulm)
    {
        int lengthOffset = cs.getOffset();
        cs.getBuffer(4); // ensure size first, avoid potential copy error
        cs.incOffset(4);
        int endOffset = cs.getOffset();
        if (ulm != null)
        {
            do
            {
                try
                {
                    endOffset = UlmToBytes2(ulm, cs.getBuffer(base_length), cs.getOffset());
                    break;
                }
                catch (ArrayIndexOutOfBoundsException ex)
                {
                    base_length += base_length / 4; // increase buffer of 25%
                    System.out.println("increasing base_length to : " + base_length);

                }
            }
            while (true);
        }
        cs.setOffset(lengthOffset);
        DataHelper.writeInteger(cs, endOffset - lengthOffset - 4);
        cs.setOffset(endOffset);
    }

    private static int UlmToBytes2(ULMessage ulm, byte[] buffer, int offset)
    {

        for (Entry<String, String> entry : ulm.entries())
        {
            String key = entry.getKey();
            int length = key.length();
            for (int i = 0; i < length; i++)
            {
                buffer[offset++] = (byte) key.charAt(i);
            }

            buffer[offset++] = 61;
            String value = entry.getValue();
            offset = encode(value, value.length(), buffer, offset);
            buffer[offset++] = 124;
        }
        return offset;
    }

    private static int encode(String value, int valueLength, byte buffer[], int offset)
    {
        int currentIndex = 0;
        for (; currentIndex < valueLength; currentIndex++)
        {
            char currentChar = value.charAt(currentIndex);
            if (currentChar == '|' || currentChar == '\003')
            {
                break;
            }
            buffer[offset++] = (byte) currentChar;
        }
        if (currentIndex == valueLength)
        {
            return offset;
        }
        for (; currentIndex < valueLength; currentIndex++)
        {
            char currentChar = value.charAt(currentIndex);
            if (currentChar == '|' || currentChar == '\003')
            {
                System.arraycopy(ESCAPE_SEQUENCE_BYTES, 0, buffer, offset, ESCAPE_SEQUENCE_LENGTH);
                offset += ESCAPE_SEQUENCE_LENGTH;
            }
            else
            {
                buffer[offset++] = (byte) currentChar;
            }
        }

        return offset;
    }
}
