import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Created by Noxid on 09-Mar-16.
 */
public class HogSplitter {

    HogSplitter(File hogfile) {
        try {
            FileInputStream fis = new FileInputStream(hogfile);
            FileChannel fc = fis.getChannel();

            ByteBuffer header = ByteBuffer.allocate(12);
            fc.read(header);
            header.flip();
            header.order(ByteOrder.LITTLE_ENDIAN);

            int unknown = header.getInt();
            int nItem = header.getInt();
            int startOffset = header.getInt(); //this is redundant

            ByteBuffer offsetTable = ByteBuffer.allocate(nItem*4);
            fc.read(offsetTable);
            offsetTable.flip();
            offsetTable.order(ByteOrder.LITTLE_ENDIAN);

            int previousOffset = offsetTable.getInt(); //this is always zero anyway..
            for (int i = 0; i < nItem; i++) {
                int nextOffset;
                if (nItem == i+1) {
                    nextOffset = (int) fc.size();
                } else {
                    nextOffset = offsetTable.getInt();
                }
                ByteBuffer itemBuf = ByteBuffer.allocate(nextOffset-previousOffset);
                fc.read(itemBuf);
                itemBuf.flip();
                itemBuf.order(ByteOrder.LITTLE_ENDIAN);
                System.out.println(i + " Found item of size " + (nextOffset-previousOffset)
                    + " at offset " + nextOffset);
                save(hogfile.getParentFile(), hogfile.getName().replace(".HOG", ""), itemBuf, i);
                previousOffset =  nextOffset;
            }

            fc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save(File parent, String name, ByteBuffer data, int index) throws IOException {
        String suffix = ".out";
        if (data.getInt(0) == 0x41) {
            suffix = ".tmd";
        }
        if (data.getInt(0) == 0x10) {
            suffix = ".tim";
        }
        File outFile = new File(parent + "/" + name + "/" + index + suffix);
        outFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(outFile);
        FileChannel fc = fos.getChannel();
        fc.write(data);
        fc.close();
    }

    public static void main(String[] args) {
        new HogSplitter(new File("./pigpen/PERM14.HOG"));
    }
}
