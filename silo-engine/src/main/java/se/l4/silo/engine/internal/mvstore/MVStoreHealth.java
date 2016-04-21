package se.l4.silo.engine.internal.mvstore;

import java.io.IOException;

import org.h2.mvstore.FileStore;
import org.h2.mvstore.MVStore;

import se.l4.silo.StorageException;
import se.l4.vibe.mapping.KeyValueMappable;
import se.l4.vibe.mapping.KeyValueReceiver;
import se.l4.vibe.probes.AbstractSampledProbe;
import se.l4.vibe.probes.SampledProbe;

public class MVStoreHealth
	implements KeyValueMappable
{
	private long currentVersion;
	
	private long fileSize;
	
	private long readBytes;
	private long readCount;
	private long writeBytes;
	private long writeCount;

	private final int fillRate;
	
	public MVStoreHealth(long currentVersion, long fileSize, long readBytes, long readCount, long writeBytes, long writeCount, int fillRate)
	{
		this.currentVersion = currentVersion;
		this.fileSize = fileSize;
		this.readBytes = readBytes;
		this.readCount = readCount;
		this.writeBytes = writeBytes;
		this.writeCount = writeCount;
		this.fillRate = fillRate;
	}

	@Override
	public void mapToKeyValues(KeyValueReceiver receiver)
	{
		receiver.add("currentVersion", currentVersion);
		receiver.add("fileSize", fileSize);
		receiver.add("readBytes", readBytes);
		receiver.add("readCount", readCount);
		receiver.add("writeBytes", writeBytes);
		receiver.add("writeCount", writeCount);
		receiver.add("fillRate", fillRate);
	}
	
	public static SampledProbe<MVStoreHealth> createProbe(MVStore store)
	{
		return new AbstractSampledProbe<MVStoreHealth>()
		{
			private long readBytes;
			private long readCount;
			private long writeBytes;
			private long writeCount;

			
			private long getFileSize(FileStore fs)
			{
				try
				{
					return fs.getFile().size();
				}
				catch(IOException e)
				{
					throw new StorageException("Unable to read size of MVStore file; " + e.getMessage(), e);
				}
			}
			
			@Override
			public MVStoreHealth peek()
			{
				long currentVersion = store.getCurrentVersion();
				FileStore fs = store.getFileStore();
				long fileSize = getFileSize(fs);
				long totalReadBytes = fs.getReadBytes();
				long totalReadCount = fs.getReadCount();
				long totalWriteBytes = fs.getWriteBytes();
				long totalWriteCount = fs.getWriteCount();
				int fillRate = fs.getFillRate();
				
				return new MVStoreHealth(
					currentVersion,
					fileSize,
					totalReadBytes - readBytes,
					totalReadCount - readCount,
					totalWriteBytes - writeBytes,
					totalWriteCount - writeCount,
					fillRate
				);
			}
			
			@Override
			protected MVStoreHealth sample0()
			{
				long currentVersion = store.getCurrentVersion();
				
				FileStore fs = store.getFileStore();
				long fileSize = getFileSize(fs);
				long totalReadBytes = fs.getReadBytes();
				long totalReadCount = fs.getReadCount();
				long totalWriteBytes = fs.getWriteBytes();
				long totalWriteCount = fs.getWriteCount();
				int fillRate = fs.getFillRate();
				
				MVStoreHealth health = new MVStoreHealth(
					currentVersion,
					fileSize,
					totalReadBytes - readBytes,
					totalReadCount - readCount,
					totalWriteBytes - writeBytes,
					totalWriteCount - writeCount,
					fillRate
				);
				
				this.readBytes = totalReadBytes;
				this.readCount = totalReadCount;
				this.writeBytes = totalWriteBytes;
				this.writeCount = totalWriteCount;
				
				return health;
			}
		};
	}

}
