package com.Revsoft.Wabbitemu.extract;

import com.Revsoft.Wabbitemu.extract.MsiDatabase.CItem;
import com.Revsoft.Wabbitemu.extract.MsiDatabase.CRef;
import com.Revsoft.Wabbitemu.extract.MsiDatabase.NFatID;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MsiHandler {
    private final MsiDatabase _db;

    public MsiHandler(MsiDatabase database) {
        this._db = database;
    }

    public byte[] GetStream(RandomAccessFile file, int index) throws IOException {
        int itemIndex = ((CRef) this._db.Refs.get(index)).Did;
        return GetStream(file, (CItem) this._db.Items.get(itemIndex), itemIndex);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public byte[] GetStream(RandomAccessFile file, CItem item, int itemIndex) throws IOException {
        boolean isLargeStream;
        int bsLog;
        long clusterSize;
        long numClusters64;
        int[] stuff;
        int sid;
        long size;
        int i;
        int i2;
        if (itemIndex != 0) {
            if (!this._db.isLargeStream(item.Size)) {
                isLargeStream = false;
                bsLog = isLargeStream ? this._db.SectorSizeBits : this._db.MiniSectorSizeBits;
                clusterSize = 1 << bsLog;
                numClusters64 = ((item.Size + clusterSize) - 1) >> bsLog;
                if (numClusters64 < 2147483648L) {
                    throw new UnsupportedOperationException("Unimplemented");
                }
                stuff = new int[((int) numClusters64)];
                sid = item.Sid;
                size = item.Size;
                if (size != 0) {
                    i = 0;
                    while (true) {
                        if (isLargeStream) {
                            long val = this._db.getMiniCluster(sid);
                            if (sid < this._db.MatSize && val < 4294967296L) {
                                i2 = i + 1;
                                stuff[i] = (int) val;
                                sid = this._db.Mat.get(sid);
                            }
                        } else if (((long) sid) < this._db.FatSize) {
                            throw new IOException("sid >= _db.FatSize");
                        } else {
                            i2 = i + 1;
                            stuff[i] = sid + 1;
                            sid = this._db.Fat.get(sid);
                        }
                        if (size <= clusterSize) {
                            break;
                        }
                        size -= clusterSize;
                        i = i2;
                    }
                }
                if (NFatID.fromInt(sid) == NFatID.kEndOfChain) {
                    throw new IOException("Not kEndOfChain");
                }
                int dataSize = (int) item.Size;
                byte[] fileData = new byte[dataSize];
                file.seek((long) (stuff[0] << bsLog));
                file.read(fileData, 0, dataSize);
                return fileData;
            }
        }
        isLargeStream = true;
        if (isLargeStream) {
        }
        clusterSize = 1 << bsLog;
        numClusters64 = ((item.Size + clusterSize) - 1) >> bsLog;
        if (numClusters64 < 2147483648L) {
            stuff = new int[((int) numClusters64)];
            sid = item.Sid;
            size = item.Size;
            if (size != 0) {
                i = 0;
                while (true) {
                    if (isLargeStream) {
                        long val2 = this._db.getMiniCluster(sid);
                        if (sid < this._db.MatSize) {
                            break;
                        }
                        break;
                    } else if (((long) sid) < this._db.FatSize) {
                        i2 = i + 1;
                        stuff[i] = sid + 1;
                        sid = this._db.Fat.get(sid);
                    } else {
                        throw new IOException("sid >= _db.FatSize");
                    }
                    if (size <= clusterSize) {
                        break;
                    }
                    size -= clusterSize;
                    i = i2;
                }
            }
            if (NFatID.fromInt(sid) == NFatID.kEndOfChain) {
                int dataSize2 = (int) item.Size;
                byte[] fileData2 = new byte[dataSize2];
                file.seek((long) (stuff[0] << bsLog));
                file.read(fileData2, 0, dataSize2);
                return fileData2;
            }
            throw new IOException("Not kEndOfChain");
        }
        throw new UnsupportedOperationException("Unimplemented");
    }
}
