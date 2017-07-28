package feflib.fates.gamedata.terrain;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* A modified version of RandomAccessFile with support for little endian,
 * writing arrays, and writing/reading label pointers from bin files.
 *
 * This code is really old at this point. The only file that still relies
 * on BinFile is FatesTerrain, but it should be phased out in favor of
 * an array-based editor eventually.
 * 
 * @author thane98
 */
public class BinFile extends RandomAccessFile 
{
	private String name;
	
    public BinFile(File file, String mode) throws FileNotFoundException 
    {
        super(file, mode);
        setName(file.getName());
    }
    
    /* Read the next four bytes as a little endian integer.
     * 
     * @return The next four bytes represented as an integer.
     */
    public int readLittleInt() throws IOException 
    {
        byte[] bytes = new byte[4];
        int x = 0;
        while (x < 4) 
        {
            bytes[x] = this.readByte();
            x++;
        }
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
    
    
    /* Read the next four bytes as a little endian short.
     * 
     * @return The next four bytes represented as a short.
     */
    public short readLittleShort() throws IOException 
    {
        byte[] bytes = new byte[2];
        int x = 0;
        while (x < 2) {
            bytes[x] = this.readByte();
            x++;
        }
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
    
    
    /* Reads the next length bytes into a byte array.
     * 
     * @param length The number of bytes to read into a byte array.
     * @return The parsed byte array.
     */
    public byte[] readByteArray(int length) throws IOException
    {
    	byte[] bytes = new byte[length];
    	for(int x = 0; x < bytes.length; x++)
    		bytes[x] = this.readByte();
    	return bytes;
    }
    
    /* Read the next length * 2 bytes into a short array.
     * 
     * @param length The number of shorts to parse.
     * 
     * @return The parsed short array.
     */
    public short[] readShortArray(int length) throws IOException
    {
    	short[] shorts = new short[length];
    	for(int x = 0; x < shorts.length; x++)
    		shorts[x] = this.readLittleShort();
    	return shorts;
    }
    
    /* Read the next length * 4 bytes into an integer array.
     * 
     * @param length The number of integers to parse.
     * 
     * @return The parsed integer array.
     */
    public int[] readIntArray(int length) throws IOException
    {
    	int[] ints = new int[length];
    	for(int x = 0; x < ints.length; x++)
    		ints[x] = this.readLittleInt();
    	return ints;
    }

    /* Reads from the current file position until a byte
     * of value 0 is found and then converts the read bytes
     * into a string using shift-jis encoding.
     * 
     * @return The parsed string encoded using shift-jis.
     */
    public String readString() throws IOException {
        byte temp;
        ArrayList<Byte> bytes = new ArrayList<>();
        while ((temp = this.readByte()) != 0) 
            bytes.add(temp);
        return new String(byteListToArray(bytes), "shift-jis");
    }
    
    /* Reads the next 4 bytes as a little endian integer, seeks to the read value,
     * calls read string, and returns to the original offset after reading the integer.
     * after the integer.
     * 
     * @return The parsed string encoded using shift-jis.
     */
    public String readStringFromPointer() throws IOException
    {
    	int offset = this.readLittleInt();
    	if(offset <= 0)
    		return "";
    	int original = (int) this.getFilePointer();
    	this.seek(offset + 0x20);
    	String parsedString = this.readString();
    	this.seek(original);
    	return parsedString;
    }

    /* Writes the given integer to the file in little endian byte order.
     * 
     * @param input The integer to write to the file.
     */
    public void writeLittleInt(int input) throws IOException 
    {
        byte[] bytes = ByteBuffer.allocate(4).putInt(input).array();
        reverseEndian(bytes);
        for (byte aByte : bytes) this.write(aByte);
    }

    /* Writes the given short to the file in little endian byte order.
     * 
     * @param input The short to write to the file.
     */
    public void writeLittleShort(short input) throws IOException
    {
    	byte[] bytes = ByteBuffer.allocate(2).putShort(input).array();
    	reverseEndian(bytes);
        for (byte aByte : bytes) this.write(aByte);
    }
    
    /* Writes each short in the given array to the file in little endian
     * byte order.
     * 
     * @param input The short array to write to the file.
     */
    public void writeShortArray(short[] input) throws IOException
    {
    	for(short s : input)
    		writeLittleShort(s);
    }
    
    
    /* Writes each byte in the given array to the file.
     * 
     * @param input The byte array to write to the file.
     */
    public void writeByteArray(byte[] input) throws IOException
    {
    	for(byte b : input)
    		writeByte(b);
    }
    
    /* Converts the file to a byte array.
     * 
     * @return The contents of the BinFile as a byte array.
     */
    public byte[] toByteArray() throws IOException 
    {
        byte[] fileBytes = new byte[(int)this.length()];
        int originalLocation = (int) this.getFilePointer();
        this.seek(0);
        this.readFully(fileBytes);
        this.seek(originalLocation);
        return fileBytes;
    }
    
    /* Converts the file to a byte array starting at the given offset.
     * 
     * @return The contents of the BinFile from the start position on as a byte array.
     */
    public byte[] toByteArray(int startPosition) throws IOException 
    {
        byte[] fileBytes = new byte[(int) (this.length() - startPosition)];
        this.seek(startPosition);
        for(int x = 0; x < fileBytes.length; x++)
        	fileBytes[x] = readByte();
        return fileBytes;
    }

	public String getName() 
	{
		return name;
	}

	public void setName(String name) 
	{
		this.name = name;
	}

    private static byte[] reverseEndian(byte[] input)
    {
        int i = 0;
        while (i < input.length / 2) {
            byte temp = input[i];
            input[i] = input[input.length - i - 1];
            input[input.length - i - 1] = temp;
            ++i;
        }
        return input;
    }

    private static byte[] byteListToArray(List<Byte> byteList)
    {
        byte[] byteArray = new byte[byteList.size()];
        int index = 0;
        while (index < byteList.size()) {
            byteArray[index] = byteList.get(index);
            ++index;
        }
        return byteArray;
    }
}

