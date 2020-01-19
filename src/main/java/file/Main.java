package file;

import java.io.File;

public class Main {
	public static void main(String[] args) {
		String filePath = "C:\\Users\\admin\\Desktop\\pic\\ww.jpg";
		String filePath2 = "C:\\Users\\admin\\Desktop\\pic\\ww2.jpg";
		String filePath3 = "C:\\Users\\admin\\Desktop\\pic\\aaa.jpg";
		File file = new File(filePath);
		File file2 = new File(filePath2);
		File file3 = new File(filePath3);
		CheckSystemFolderSum check = new CheckSystemFolderSum();
		String checkMd5 = CheckSystemFolderSum.checkMd5(file);
		String checkMd52 = CheckSystemFolderSum.checkMd5(file2);
		String checkMd53 = CheckSystemFolderSum.checkMd5(file3);

		System.out.println(checkMd5);
		System.out.println(checkMd52);
		System.out.println(checkMd53);
	}
}
