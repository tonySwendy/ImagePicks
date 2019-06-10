package com.lzy.imagepicker;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.lzy.imagepicker.bean.ImageFolder;
import com.lzy.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ================================================
 * ��    �ߣ�jeasonlzy������Ң Github��ַ��https://github.com/jeasonlzy0216
 * ��    ����1.0
 * �������ڣ�2016/5/19
 * ��    ���������ֻ�ͼƬʵ����
 * �޶���ʷ��
 * ================================================
 */
public class ImageDataSource implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ALL = 0;         //��������ͼƬ
    public static final int LOADER_CATEGORY = 1;    //�������ͼƬ
    private final String[] IMAGE_PROJECTION = {     //��ѯͼƬ��Ҫ��������
            MediaStore.Images.Media.DISPLAY_NAME,   //ͼƬ����ʾ����  aaa.jpg
            MediaStore.Images.Media.DATA,           //ͼƬ����ʵ·��  /storage/emulated/0/pp/downloader/wallpaper/aaa.jpg
            MediaStore.Images.Media.SIZE,           //ͼƬ�Ĵ�С��long��  132492
            MediaStore.Images.Media.WIDTH,          //ͼƬ�Ŀ�ȣ�int��  1920
            MediaStore.Images.Media.HEIGHT,         //ͼƬ�ĸ߶ȣ�int��  1080
            MediaStore.Images.Media.MIME_TYPE,      //ͼƬ������     image/jpeg
            MediaStore.Images.Media.DATE_ADDED};    //ͼƬ����ӵ�ʱ�䣬long��  1450518608

    private FragmentActivity activity;
    private OnImagesLoadedListener loadedListener;                     //ͼƬ������ɵĻص��ӿ�
    private ArrayList<ImageFolder> imageFolders = new ArrayList<>();   //���е�ͼƬ�ļ���
    private int mLoadedCount = 0;

    /**
     * @param activity       ���ڳ�ʼ��LoaderManager����Ҫ���ݵ�2.3
     * @param path           ָ��ɨ����ļ���Ŀ¼������Ϊ null����ʾɨ������ͼƬ
     * @param loadedListener ͼƬ������ɵļ���
     */
    public ImageDataSource(FragmentActivity activity, String path, OnImagesLoadedListener loadedListener) {
        this.activity = activity;
        this.loadedListener = loadedListener;
        mLoadedCount=0;

        LoaderManager loaderManager = activity.getSupportLoaderManager();
        if (path == null) {
            loaderManager.initLoader(LOADER_ALL, null, this);//�������е�ͼƬ
        } else {
            //����ָ��Ŀ¼��ͼƬ
            Bundle bundle = new Bundle();
            bundle.putString("path", path);
            loaderManager.initLoader(LOADER_CATEGORY, bundle, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        //ɨ������ͼƬ
        if (id == LOADER_ALL)
            cursorLoader = new CursorLoader(activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION, null, null, IMAGE_PROJECTION[6] + " DESC");
        //ɨ��ĳ��ͼƬ�ļ���
        if (id == LOADER_CATEGORY)
            cursorLoader = new CursorLoader(activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION, IMAGE_PROJECTION[1] + " like '%" + args.getString("path") + "%'", null, IMAGE_PROJECTION[6] + " DESC");

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() == 0) {
            return;
        }
        if (mLoadedCount == data.getCount()) {
            return;
        }
        imageFolders.clear();
        mLoadedCount = data.getCount();
        ArrayList<ImageItem> allImages = new ArrayList<>();   //����ͼƬ�ļ���,�����ļ���
        while (data.moveToNext()) {
            //��ѯ����
            String imageName = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
            String imagePath = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));

            File file = new File(imagePath);
            if (!file.exists() || file.length() <= 0) {
                continue;
            }

            long imageSize = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
            int imageWidth = data.getInt(data.getColumnIndexOrThrow(IMAGE_PROJECTION[3]));
            int imageHeight = data.getInt(data.getColumnIndexOrThrow(IMAGE_PROJECTION[4]));
            String imageMimeType = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[5]));
            long imageAddTime = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[6]));
            //��װʵ��
            ImageItem imageItem = new ImageItem();
            imageItem.name = imageName;
            imageItem.path = imagePath;
            imageItem.size = imageSize;
            imageItem.width = imageWidth;
            imageItem.height = imageHeight;
            imageItem.mimeType = imageMimeType;
            imageItem.addTime = imageAddTime;
            allImages.add(imageItem);
            //���ݸ�·��������ͼƬ
            File imageFile = new File(imagePath);
            File imageParentFile = imageFile.getParentFile();
            ImageFolder imageFolder = new ImageFolder();
            imageFolder.name = imageParentFile.getName();
            imageFolder.path = imageParentFile.getAbsolutePath();

            if (!imageFolders.contains(imageFolder)) {
                ArrayList<ImageItem> images = new ArrayList<>();
                images.add(imageItem);
                imageFolder.cover = imageItem;
                imageFolder.images = images;
                imageFolders.add(imageFolder);
            } else {
                imageFolders.get(imageFolders.indexOf(imageFolder)).images.add(imageItem);
            }
        }
        //��ֹû��ͼƬ���쳣
        if (data.getCount() > 0 && allImages.size() > 0) {
            //��������ͼƬ�ļ���
            ImageFolder allImagesFolder = new ImageFolder();
            allImagesFolder.name = activity.getResources().getString(R.string.ip_all_images);
            allImagesFolder.path = "/";
            allImagesFolder.cover = allImages.get(0);
            allImagesFolder.images = allImages;
            imageFolders.add(0, allImagesFolder);  //ȷ����һ��������ͼƬ
        }
        //�ص��ӿڣ�֪ͨͼƬ����׼�����
        ImagePicker.getInstance().setImageFolders(imageFolders);
        loadedListener.onImagesLoaded(imageFolders);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        System.out.println("--------");
    }

    /**
     * ����ͼƬ������ɵĻص��ӿ�
     */
    public interface OnImagesLoadedListener {
        void onImagesLoaded(List<ImageFolder> imageFolders);
    }
}
