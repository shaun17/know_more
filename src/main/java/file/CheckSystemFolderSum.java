package file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CheckSystemFolderSum {
	// 所有文件md5总和
	private static String fileSum = "";

	/**
	 * 遍历文件夹下的所有文件(递归) 并对每个文件计算md5值 得到所有文件的md5值之和
	 * 
	 * @param file
	 *            软件系统的根文件夹 , suffix 目录文件后缀(以该后缀结尾的目录不会遍历和计算md5值)
	 * @return 系统所有文件md5之和
	 */
	public String traverseFolder(File file, String suffix) {

		if (file == null) {
			throw new NullPointerException("遍历路径为空路径或非法路径");
		}

		if (file.exists()) { // 判断文件或目录是否存在

			File[] files = file.listFiles();

			if (files.length == 0) { // 文件夹为空
				return null;
			} else {
				for (File f : files) { // 遍历文件夹

					if (f.isDirectory()) { // 判断是否是目录

						if (!(f.getName().endsWith(".no"))) { // 如果不是以.no结尾的目录 则计算该目录下的文件的md5值

							// 递归遍历
							traverseFolder(f, suffix);
						}

					} else {
						// 得到文件的md5值
						String string = checkMd5(f);
						// 将每个文件的md5值相加
						fileSum += string;
					}
				}
			}

		} else {
			return null; // 目录不存在
		}

		return fileSum; // 返回所有文件md5值字符串之和
	}

	/**
	 * 检验文件生成唯一的md5值 作用：检验文件是否已被修改
	 * 
	 * @param file
	 *            需要检验的文件
	 * @return 该文件的md5值
	 */
	static String checkMd5(File file) {

		// 若输入的参数不是一个文件 则抛出异常
		if (!file.isFile()) {
			throw new NumberFormatException("参数错误！请输入校准文件。");
		}

		// 定义相关变量
		FileInputStream fis = null;
		byte[] rb = null;
		DigestInputStream digestInputStream = null;

		try {

			fis = new FileInputStream(file);
			MessageDigest md5 = MessageDigest.getInstance("md5");
			digestInputStream = new DigestInputStream(fis, md5);
			byte[] buffer = new byte[4096];

			while (digestInputStream.read(buffer) > 0)
				;

			md5 = digestInputStream.getMessageDigest();
			rb = md5.digest();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < rb.length; i++) {
			String a = Integer.toHexString(0XFF & rb[i]);
			if (a.length() < 2) {
				a = '0' + a;
			}
			sb.append(a);
		}
		return sb.toString(); // 得到md5值
	}
}
