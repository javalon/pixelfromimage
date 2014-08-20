PixelFromImage
==============

This project is an **Android** Activity which return a touched pixel color from an image. 
You can choose the image will load from **Android** Gallery or from the camera. The implemented UI allows to do zooming and panning to increase the precision of the pixel selection.

**TestPixelFromImage** is a **Android** application to test **PixelFromImage** as library.

## Instalation

You only import **PixelFromImage** into your Eclipse workspace and set **PixelFromImage** as library in your project properties.

## Usage

You only need instantiate a Intent and implement **onActivityResult** method in your Application Activity.

```
Intent intent = new Intent(getApplicationContext(),PickerColorFromImage.class);
startActivityForResult(intent, 1);
```

In **onActivityResult** method

```
Bundle bundle= data.getExtras();
txtColor.setText(Integer.toHexString(bundle.getInt("color")));
```

## Refs
- Get touched pixel color of scaled ImageView <http://android-er.blogspot.com.es/2012/10/get-touched-pixel-color-of-scaled.html>