/* Sample skeleton for proxy */

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

class FileInfo{
	File file;
	RandomAccessFile raf;
	public FileInfo(File f, RandomAccessFile r) {
		this.file=f;
		this.raf=r;
	}
}


class Proxy {
	static HashMap<Integer,FileInfo> fd2Raf = new HashMap<Integer,FileInfo>();
	public static final int MAXFDSIZE = 1000;
	private static class FileHandler implements FileHandling {

		public int open( String path, OpenOption o ) {
			File f = new File(path);
			int fd = fd2Raf.size()+1;
			if (fd > MAXFDSIZE) {
				return Errors.EMFILE;
			}
			switch(o) {
				case CREATE:
					System.err.println("CREATE");
					
					try {
						if (!f.createNewFile()) {
							return Errors.EPERM;
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}

					try {
						RandomAccessFile raf_c = new RandomAccessFile(f,"rw");
						FileInfo fi_c = new FileInfo(f,raf_c);
						fd2Raf.put(fd,fi_c);
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
									
					break;
				case CREATE_NEW:
					System.err.println("CREATE_NEW");
					if (f.exists()) {
						return Errors.EEXIST;
					}
					try {
						if (!f.createNewFile()) {
							return Errors.EPERM;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					try {
						RandomAccessFile raf_cn = new RandomAccessFile(f,"rw");
						FileInfo fi_cn = new FileInfo(f,raf_cn);
						fd2Raf.put(fd,fi_cn);
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					break;
				case READ:
					System.err.println("READ");
					if (!f.exists()) {
						return Errors.ENOENT;
					}
					
					try {
						RandomAccessFile raf_r = new RandomAccessFile(f,"r");
						FileInfo fi_r = new FileInfo(f,raf_r);
						fd2Raf.put(fd,fi_r);
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					
					break;
				case WRITE:
					System.err.println("WRITE");
					if (f.isDirectory()) {
						return Errors.EISDIR;
					}
					if (!f.exists()) {
						return Errors.ENOENT;
					}
					
					try {
						RandomAccessFile raf_w = new RandomAccessFile(f,"rw");
						FileInfo fi_w = new FileInfo(f,raf_w);
						fd2Raf.put(fd,fi_w);
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					
					break;
				default:
					return Errors.EINVAL;
			}
			return fd;
		}
		
		//EBADF
		public int close( int fd ) {
			FileInfo raf = fd2Raf.get(fd);
			if(raf == null) {
				return Errors.EBADF;
			}
			try {
				raf.raf.close();
			} catch (IOException e) {
				e.printStackTrace();
				return -1; //?
			}
			fd2Raf.remove(fd);
			return 0;
		}	
		
		
		//EBADF,EINVAL,EPERM
		public long write( int fd, byte[] buf ) {
			FileInfo raf = fd2Raf.get(fd);
			if(raf == null) {
				return Errors.EBADF;
			}
			if (!raf.file.canWrite()) {
				return Errors.EINVAL;
			}
			try {
				raf.raf.write(buf);
			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
			return buf.length;
		}

		//EBADF,ENINVAL,EISDIR
		public long read( int fd, byte[] buf ) {
			FileInfo raf = fd2Raf.get(fd);
			if (raf == null) {
				return Errors.EBADF;
			}
			if (raf.file.isDirectory()) {
				return Errors.EISDIR;
			}
			if (!raf.file.canRead()) {
				return Errors.EINVAL;
			}
			int ret = -1;
			try {
				ret = raf.raf.read(buf);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return ret;
		}

		public long lseek( int fd, long pos, LseekOption o ) {
			FileInfo raf = fd2Raf.get(fd);
			long offset = -1;
			if (raf == null) {
				return Errors.EBADF;
			}
			switch(o) {
				case FROM_CURRENT:
					try {
						raf.raf.seek(raf.raf.getFilePointer() + pos);
						offset = raf.raf.getFilePointer() + pos;
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				case FROM_END:
					try {
						raf.raf.seek(raf.raf.length() - pos);
						offset = raf.raf.length() - pos;
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				case FROM_START:					
					try {
						raf.raf.seek(pos);
						offset = pos;
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				default:
					return Errors.EINVAL;
			}
			return offset;
		}

		public int unlink( String path ) {
			File f = new File(path);
			if (f.isDirectory()) {
				return Errors.EISDIR;
			}
			if (!f.isFile()) {
				return Errors.ENOENT;
			}
			for (Entry<Integer, FileInfo> c : fd2Raf.entrySet()) {
				if (c.getValue().file.equals(f)) {
					fd2Raf.remove(c.getKey());
				}
			}
			f.delete();
			if (f.exists()) {
				return -1;
			}
			return 0;
		}

		public void clientdone() {
			//close all files recylce
			for (Entry<Integer,FileInfo> c : fd2Raf.entrySet()) {
				try {
					c.getValue().raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				//Reuse fds
				fd2Raf.remove(c.getKey());
			}
			return;
		}

	}
	
	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Hello World");
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
