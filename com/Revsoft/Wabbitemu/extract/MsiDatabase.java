package com.Revsoft.Wabbitemu.extract;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MsiDatabase {
    private static final byte[] SIGNATURE = new byte[]{(byte) -48, (byte) -49, (byte) 17, (byte) -32, (byte) -95, (byte) -79, (byte) 26, (byte) -31};
    private static final char kCharCloseBracket = ']';
    private static final char kCharOpenBracket = '[';
    private static final int kHeaderSize = 512;
    private static final byte[] kMspSequence = new byte[]{(byte) 64, (byte) 72, (byte) -106, (byte) 69, (byte) 108, (byte) 62, (byte) -28, (byte) 69, (byte) -26, (byte) 66, (byte) 22, (byte) 66, (byte) 55, (byte) 65, (byte) 39, (byte) 65, (byte) 55, (byte) 65};
    private static final int kNameSizeMax = 64;
    private static final int kNoDid = -1;
    private static final int k_Msi_CharMask = 63;
    private static final String k_Msi_Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._";
    private static final int k_Msi_NumBits = 6;
    private static final int k_Msi_NumChars = 64;
    private static final char k_Msi_SpecChar = '!';
    private static final int k_Msi_StartUnicodeChar = 14336;
    private static final int k_Msi_UnicodeRange = 4160;
    IntBuffer Fat;
    long FatSize;
    public List<CItem> Items = new ArrayList();
    int LongStreamMinSize;
    int MainSubfile;
    IntBuffer Mat;
    int MatSize;
    int MiniSectorSizeBits;
    IntBuffer MiniSids;
    int NumSectorsInMiniStream;
    long PhySize;
    public List<CRef> Refs = new ArrayList();
    int SectorSizeBits;
    EType Type;

    public static class CItem {
        public int LeftDid;
        ByteBuffer Name;
        public int RightDid;
        public int Sid;
        public long Size;
        public int SonDid;
        public NItemType Type;

        public boolean isEmpty() {
            return this.Type == NItemType.kEmpty;
        }

        public boolean isDir() {
            return this.Type == NItemType.kStorage || this.Type == NItemType.kRootStorage;
        }

        public void Parse(ByteBuffer p, boolean mode64bit) {
            byte[] bytes = new byte[p.capacity()];
            p.get(bytes, p.position(), p.capacity() - p.position());
            this.Name = ByteBuffer.wrap(bytes);
            this.Name.position(p.position());
            this.Type = NItemType.fromInt(p.get(66));
            this.LeftDid = MsiDatabase.Get32(p, 68);
            this.RightDid = MsiDatabase.Get32(p, 72);
            this.SonDid = MsiDatabase.Get32(p, 76);
            this.Sid = MsiDatabase.Get32(p, 116);
            this.Size = (long) MsiDatabase.Get32(p, 120);
            if (mode64bit) {
                this.Size |= ((long) MsiDatabase.Get32(p, 124)) << 32;
            }
        }

        public String getRealName() {
            return MsiDatabase.convertName(this.Name).mName;
        }

        public String toString() {
            return MsiDatabase.convertName(this.Name).mName;
        }
    }

    public static class CRef {
        public int Did;
        public int Parent;
    }

    public enum EType {
        k_Type_Common,
        k_Type_Msi,
        k_Type_Msp,
        k_Type_Doc,
        k_Type_Ppt,
        k_Type_Xls
    }

    private static class MsiName {
        public final boolean mIsMsiName;
        public final String mName;

        public MsiName(String name, boolean isMsiName) {
            this.mIsMsiName = isMsiName;
            this.mName = name;
        }

        public String toString() {
            return String.format("Name: %s, isMsiName: %s", new Object[]{this.mName, Boolean.valueOf(this.mIsMsiName)});
        }
    }

    public enum NFatID {
        kFree(-1),
        kEndOfChain(-2),
        kFatSector(-3),
        kMatSector(-4),
        kMaxValue(-6);
        
        final int mVal;

        private NFatID(int val) {
            this.mVal = val;
        }

        public static boolean compare(long val, NFatID id) {
            return val < ((long) id.mVal);
        }

        public static NFatID fromInt(int val) {
            for (NFatID itemType : values()) {
                if (itemType.mVal == val) {
                    return itemType;
                }
            }
            return null;
        }
    }

    public enum NItemType {
        kEmpty(0),
        kStorage(1),
        kStream(2),
        kLockBytes(3),
        kProperty(4),
        kRootStorage(5);
        
        final int mVal;

        private NItemType(int val) {
            this.mVal = val;
        }

        public static NItemType fromInt(int val) {
            for (NItemType itemType : values()) {
                if (itemType.mVal == val) {
                    return itemType;
                }
            }
            return null;
        }
    }

    boolean addNode(int parent, int did) {
        if (did == -1) {
            return true;
        }
        if (did >= this.Items.size()) {
            return false;
        }
        CItem item = (CItem) this.Items.get(did);
        if (item.isEmpty()) {
            return false;
        }
        CRef ref = new CRef();
        ref.Parent = parent;
        ref.Did = did;
        int index = this.Refs.size();
        this.Refs.add(ref);
        if (this.Refs.size() > this.Items.size()) {
            return false;
        }
        if (!addNode(parent, item.LeftDid)) {
            return false;
        }
        if (!addNode(parent, item.RightDid)) {
            return false;
        }
        if (!item.isDir() || addNode(index, item.SonDid)) {
            return true;
        }
        return false;
    }

    private void updatePhySize(long val) {
        if (this.PhySize < val) {
            this.PhySize = val;
        }
    }

    public boolean isLargeStream(long size) {
        return size >= ((long) this.LongStreamMinSize);
    }

    public long getItemPackSize(long size) {
        long mask = (1 << (isLargeStream(size) ? this.SectorSizeBits : this.MiniSectorSizeBits)) - 1;
        return (size + mask) & (-1 ^ mask);
    }

    public long getMiniCluster(int sid) throws IOException {
        int subBits = this.SectorSizeBits - this.MiniSectorSizeBits;
        int fid = sid >> subBits;
        if (fid < this.NumSectorsInMiniStream) {
            return ((((long) this.MiniSids.get(fid)) + 1) << subBits) + ((long) (((1 << subBits) - 1) & sid));
        }
        throw new IOException("Fid: " + fid + " NumSectorsInMiniStream " + this.NumSectorsInMiniStream);
    }

    private void readSector(RandomAccessFile inStream, ByteBuffer buf, int sectorSizeBits, long sid) throws IOException {
        updatePhySize((2 + sid) << sectorSizeBits);
        inStream.seek((1 + sid) << sectorSizeBits);
        readStream(inStream, buf, (long) (1 << sectorSizeBits));
    }

    private long readStream(RandomAccessFile inStream, ByteBuffer buf, long processedSize) throws IOException {
        long processedSizeLoc = (long) inStream.read(buf.array(), 0, (int) processedSize);
        if (processedSizeLoc == processedSize) {
            return processedSize;
        }
        throw new IOException("processedSizeLoc != processedSize " + processedSizeLoc + " " + processedSize);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void open(RandomAccessFile inStream) throws IOException {
        this.MainSubfile = -1;
        this.PhySize = 512;
        ByteBuffer header = ByteBuffer.allocate(512);
        readStream(inStream, header, 512);
        byte[] signature = Arrays.copyOfRange(header.array(), 0, SIGNATURE.length);
        if (!Arrays.equals(signature, SIGNATURE)) {
            throw new IOException("Invalid signature " + Arrays.toString(signature));
        } else if (Get16(header, 26) > 4) {
            throw new IOException("Get16(header, 0x1A) = " + Get16(header, 26));
        } else if (Get16(header, 28) != 65534) {
            throw new IOException("Get16(header, 0x1C) = " + Get16(header, 28));
        } else {
            int sectorSizeBits = Get16(header, 30);
            boolean mode64bit = sectorSizeBits >= 12;
            if (mode64bit) {
                throw new IOException("Can't handle 64 bit file");
            }
            int miniSectorSizeBits = Get16(header, 32);
            this.SectorSizeBits = sectorSizeBits;
            this.MiniSectorSizeBits = miniSectorSizeBits;
            if (sectorSizeBits > 24 || sectorSizeBits < 7 || miniSectorSizeBits > 24 || miniSectorSizeBits < 2 || miniSectorSizeBits > sectorSizeBits) {
                throw new IOException("Sector size wrong " + sectorSizeBits + " " + miniSectorSizeBits);
            }
            int numSectorsForFAT = Get32(header, 44);
            this.LongStreamMinSize = Get32(header, 56);
            int sectSize = 1 << sectorSizeBits;
            ByteBuffer sect = ByteBuffer.allocate(sectSize);
            int ssb2 = sectorSizeBits - 2;
            int numSidsInSec = 1 << ssb2;
            long numFatItems = ((long) numSectorsForFAT) << ssb2;
            if ((numFatItems >> ssb2) != ((long) numSectorsForFAT)) {
                throw new IOException("Invalid fat items " + (numFatItems >> ssb2));
            }
            this.FatSize = numFatItems;
            int numSectorsForBat = Get32(header, 72);
            int numBatItems = (numSectorsForBat << ssb2) + 109;
            if (numBatItems < 109 || ((numBatItems - 109) >> ssb2) != numSectorsForBat) {
                throw new IOException("Num bat items invalid " + numBatItems);
            }
            IntBuffer bat = IntBuffer.allocate(numBatItems);
            int i = 0;
            while (i < 109) {
                bat.put(i, Get32(header, (i * 4) + 76));
                i++;
            }
            int sid = Get32(header, 68);
            for (int s = 0; s < numSectorsForBat; s++) {
                bat.position(i);
                readIDs(inStream, sect, sectorSizeBits, (long) sid, bat.slice());
                i += numSidsInSec - 1;
                sid = bat.get(i);
            }
            numBatItems = i;
            this.Fat = IntBuffer.allocate((int) numFatItems);
            int j = 0;
            i = 0;
            while (((long) i) < numFatItems) {
                if (j >= numBatItems) {
                    throw new IOException("j >= numBatItems " + numBatItems);
                }
                this.Fat.position(i);
                readIDs(inStream, sect, sectorSizeBits, (long) bat.get(j), this.Fat.slice());
                j++;
                i += numSidsInSec;
            }
            numFatItems = (long) i;
            this.FatSize = numFatItems;
            int numSectorsForMat = Get32(header, 64);
            int numMatItems = numSectorsForMat << ssb2;
            if ((numMatItems >> ssb2) != numSectorsForMat) {
                throw new IOException("numMatItems >> ssb2 " + (numMatItems >> ssb2));
            }
            this.Mat = IntBuffer.allocate(numMatItems);
            sid = Get32(header, 60);
            for (i = 0; i < numMatItems; i += numSidsInSec) {
                this.Mat.position(i);
                readIDs(inStream, sect, sectorSizeBits, (long) sid, this.Mat.slice());
                if (((long) sid) >= numFatItems) {
                    throw new IOException("sid >= numFatItems " + sid + " " + numFatItems);
                }
                sid = this.Fat.get(sid);
            }
            if (NFatID.fromInt(sid) != NFatID.kEndOfChain) {
                throw new IOException("NFatId invalid" + sid);
            }
            byte[] used = new byte[((int) numFatItems)];
            for (i = 0; ((long) i) < numFatItems; i++) {
                used[i] = (byte) 0;
            }
            sid = Get32(header, 48);
            while (((long) sid) < numFatItems) {
                if (used[sid] != (byte) 0) {
                    throw new IOException("used[sid] != 0 " + used[sid]);
                }
                CItem item;
                used[sid] = (byte) 1;
                readSector(inStream, sect, sectorSizeBits, (long) sid);
                for (i = 0; i < sectSize; i += 128) {
                    item = new CItem();
                    sect.position(i);
                    item.Parse(sect.slice(), mode64bit);
                    this.Items.add(item);
                }
                sid = this.Fat.get(sid);
                if (NFatID.fromInt(sid) == NFatID.kEndOfChain) {
                    CItem root = (CItem) this.Items.get(0);
                    long numSatSects64 = ((root.Size + ((long) sectSize)) - 1) >> sectorSizeBits;
                    if (NFatID.compare(numSatSects64, NFatID.kMaxValue)) {
                        throw new IOException("Invalid numSatSects64 " + numSatSects64);
                    }
                    int numSectorsInMiniStream = (int) numSatSects64;
                    this.NumSectorsInMiniStream = numSectorsInMiniStream;
                    this.MiniSids = IntBuffer.allocate(numSectorsInMiniStream);
                    long matSize64 = ((root.Size + (1 << miniSectorSizeBits)) - 1) >> miniSectorSizeBits;
                    if (NFatID.compare(matSize64, NFatID.kMaxValue)) {
                        throw new IOException("Invalid matSize64 " + matSize64);
                    }
                    this.MatSize = (int) matSize64;
                    if (numMatItems < this.MatSize) {
                        throw new IOException("numMatItems < MatSize " + numMatItems + " " + this.MatSize);
                    }
                    sid = root.Sid;
                    i = 0;
                    while (NFatID.fromInt(sid) != NFatID.kEndOfChain) {
                        if (i >= numSectorsInMiniStream) {
                            throw new IOException("i >= numSectorsInMiniStream" + i + " " + numSectorsInMiniStream);
                        }
                        this.MiniSids.put(i, sid);
                        if (((long) sid) >= numFatItems) {
                            throw new IOException("sid >= nuMFatItems " + sid + " " + numFatItems);
                        }
                        sid = this.Fat.get(sid);
                        i++;
                    }
                    if (i != numSectorsInMiniStream) {
                        throw new IOException("i != numSectorsInMiniStream " + i + " " + numSectorsInMiniStream);
                    }
                    int t;
                    addNode(-1, root.SonDid);
                    int numCabs = 0;
                    for (i = 0; i < this.Refs.size(); i++) {
                        item = (CItem) this.Items.get(((CRef) this.Refs.get(i)).Did);
                        if (!item.isDir() && numCabs <= 1) {
                            MsiName msiName = convertName(item.Name);
                            String name = msiName.mName;
                            if (msiName.mIsMsiName && !name.isEmpty()) {
                                boolean isMsiSpec = name.charAt(0) == k_Msi_SpecChar;
                                if (name.length() >= 4) {
                                }
                                if (!isMsiSpec && name.length() >= 3) {
                                    if (!name.substring(name.length() - 3).equalsIgnoreCase("exe")) {
                                    }
                                    numCabs++;
                                    this.MainSubfile = i;
                                }
                            }
                        }
                    }
                    if (numCabs > 1) {
                        this.MainSubfile = -1;
                    }
                    for (t = 0; t < this.Items.size(); t++) {
                        Update_PhySize_WithItem(t);
                    }
                    for (t = 0; t < this.Items.size(); t++) {
                        item = (CItem) this.Items.get(t);
                        if (isMsiName(item.Name)) {
                            this.Type = EType.k_Type_Msi;
                            boolean isValid = true;
                            for (int aaa = 0; aaa < kMspSequence.length; aaa++) {
                                if (kMspSequence[aaa] != item.Name.get(aaa)) {
                                    isValid = false;
                                    break;
                                }
                            }
                            if (isValid) {
                                this.Type = EType.k_Type_Msp;
                                return;
                            }
                        } else if (areEqualNames(item.Name, "WordDocument")) {
                            this.Type = EType.k_Type_Doc;
                            return;
                        } else if (areEqualNames(item.Name, "PowerPoint Document")) {
                            this.Type = EType.k_Type_Ppt;
                            return;
                        } else if (areEqualNames(item.Name, "Workbook")) {
                            this.Type = EType.k_Type_Xls;
                            return;
                        }
                    }
                    return;
                }
            }
            throw new IOException("sid >= numFatItems " + sid + " " + numFatItems);
        }
    }

    public String GetItemPath(int index) {
        boolean isEmpty = true;
        StringBuilder builder = new StringBuilder();
        while (index != -1) {
            CRef ref = (CRef) this.Refs.get(index);
            CItem item = (CItem) this.Items.get(ref.Did);
            if (!isEmpty) {
                builder.insert(0, '/');
                isEmpty = false;
            }
            builder.insert(0, convertName(item.Name));
            index = ref.Parent;
        }
        return builder.toString();
    }

    private static boolean areEqualNames(ByteBuffer rawName, String asciiName) {
        for (int i = 0; i < 32; i++) {
            int c = Get16(rawName, i * 2);
            if (c != asciiName.charAt(i)) {
                return false;
            }
            if (c == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMsiName(ByteBuffer p) {
        int c = Get16(p, 0);
        if (c < k_Msi_StartUnicodeChar || c > 18496) {
            return false;
        }
        return true;
    }

    private boolean Update_PhySize_WithItem(int index) {
        CItem item = (CItem) this.Items.get(index);
        boolean isLargeStream = index == 0 || isLargeStream(item.Size);
        if (!isLargeStream) {
            return true;
        }
        int bsLog = this.SectorSizeBits;
        int clusterSize = 1 << bsLog;
        if ((((item.Size + ((long) clusterSize)) - 1) >> bsLog) >= -2147483648L) {
            return false;
        }
        int sid = item.Sid;
        long size = item.Size;
        if (size != 0) {
            while (((long) sid) < this.FatSize) {
                updatePhySize((((long) sid) + 2) << bsLog);
                sid = this.Fat.get(sid);
                if (size > ((long) clusterSize)) {
                    size -= (long) clusterSize;
                }
            }
            return false;
        }
        if (NFatID.fromInt(sid) == NFatID.kEndOfChain) {
            return true;
        }
        return false;
    }

    private static MsiName convertName(ByteBuffer p) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 64; i += 2) {
            char c = (char) Get16(p, i);
            if (c == '\u0000') {
                break;
            }
            s.append(c);
        }
        String name = s.toString();
        String msiName = compoundMsiNameToFileName(name);
        if (msiName != null) {
            return new MsiName(msiName, true);
        }
        return new MsiName(CompoundNameToFileName(name), false);
    }

    private static String CompoundNameToFileName(String s) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < ' ') {
                res.append(kCharOpenBracket);
                res.append(Integer.toString(c));
                res.append(kCharCloseBracket);
            } else {
                res.append(c);
            }
        }
        return res.toString();
    }

    private static String compoundMsiNameToFileName(String name) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < '㠀' || c > '䡀') {
                return null;
            }
            c = (char) (c - 14336);
            int c0 = c & k_Msi_CharMask;
            int c1 = c >> 6;
            if (c1 <= 64) {
                builder.append(k_Msi_Chars.charAt(c0));
                if (c1 == 64) {
                    break;
                }
                builder.append(k_Msi_Chars.charAt(c1));
            } else {
                builder.append(k_Msi_SpecChar);
            }
        }
        return builder.toString();
    }

    private void readIDs(RandomAccessFile inStream, ByteBuffer buf, int sectorSizeBits, long sid, IntBuffer dest) throws IOException {
        readSector(inStream, buf, sectorSizeBits, sid);
        int sectorSize = 1 << sectorSizeBits;
        for (int t = 0; t < sectorSize; t += 4) {
            dest.put(t / 4, Get32(buf, t));
        }
    }

    private static int Get16(ByteBuffer header, int pos) {
        int val = header.getChar(pos);
        return ((val & 255) << 8) + (val >> 8);
    }

    private static int Get32(ByteBuffer header, int pos) {
        return Get16(header, pos) + (Get16(header, pos + 2) << 16);
    }
}
