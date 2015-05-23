SwissKnife [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.arasthel/swissknife/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.arasthel/swissknife)
==========

[![Join the chat at https://gitter.im/Arasthel/SwissKnife](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Arasthel/SwissKnife?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


<div align="center"><img src="./SwissKnife.png"></div>

A multi-purpose *Groovy* library containing view injection and threading for Android using annotations. It's based on both [ButterKnife](https://github.com/JakeWharton/butterknife/) and [AndroidAnnotations](https://github.com/excilys/androidannotations).

With **SwissKnife** you can:

* Inject views dynamically on *any Object* as long a you have a View to find them. No more ``findViewById`` and **you don't have to extend any classes**.
* Add callback methods to several actions using ``@OnClick``, ``@OnItemClick``, etc.
* Execute methods in the UI Thread or a background one using ``@OnUIThread`` and ``@OnBackground``.
* Make your variables persistent across state changes **without messing with** `onSaveInstanceState`.
* Make anything Parcelable with the `@Parcelable` annotation - which can be used with `@SaveInstance` to automatize data persistance. **NO MORE PARCELABLES! YAY!**
* Read intent extras automatically with `@Extra` annotation.

You can see an example here:

```groovy
class MyActivity extends Activity {

  @Extra("api_key")
  String apiKey

  @SaveInstance
  public String myString;

  @OnClick(R.id.button)
  public void onButtonClicked(Button button) {
    Toast.makeText(this, "Button clicked", Toast.LENGTH_SHOT).show();
  }

  @OnBackground
  public void doSomeProcessing(URL url) {
    // Contents will be executed on background
    ...
  }

  @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // This must be called for injection of views and callbacks to take place
        SwissKnife.inject(this);

        // This must be called for saved state restoring
        SwissKnife.restoreState(this, savedInstanceState);

        // This mus be called for automatic parsing of intent extras
        SwissKnife.loadExtras(this)
    }
}
```

## Collaborators

I'd really want to thank:

* [Dexafree](https://github.com/dexafree) for adding `@SaveInstance` annotation and helping with the testing.
* [Eugene Kamenev](https://github.com/eugene-kamenev) for adding DSL functionality to *[android-dsl](https://github.com/Arasthel/SwissKnife/tree/android-dsl)* branch.

## Documentation

For more info, please [read the wiki pages](https://github.com/Arasthel/SwissKnife/wiki). They contain explanations about how to use the library and the available annotations:

* [@OnClick](https://github.com/Arasthel/SwissKnife/wiki/@OnClick)
* [@OnLongClick](https://github.com/Arasthel/SwissKnife/wiki/@OnLongClick)
* [@OnItemClick](https://github.com/Arasthel/SwissKnife/wiki/@OnItemClick)
* [@OnItemLongClick](https://github.com/Arasthel/SwissKnife/wiki/@OnItemLongClick)
* [@OnItemSelected](https://github.com/Arasthel/SwissKnife/wiki/@OnItemSelected)
* [@OnChecked](https://github.com/Arasthel/SwissKnife/wiki/@OnChecked)
* [@OnFocusChanged](https://github.com/Arasthel/SwissKnife/wiki/@OnFocusChanged)
* [@OnTouch](https://github.com/Arasthel/SwissKnife/wiki/@OnTouch)
* [@OnPageChanged](https://github.com/Arasthel/SwissKnife/wiki/@OnPageChanged)
* [@OnTextChanged](https://github.com/Arasthel/SwissKnife/wiki/@OnTextChanged)
* [@OnEditorAction](https://github.com/Arasthel/SwissKnife/wiki/@OnEditorAction)
* [@OnUIThread](https://github.com/Arasthel/SwissKnife/wiki/@OnUIThread)
* [@OnBackground](https://github.com/Arasthel/SwissKnife/wiki/@OnBackground)
* [@SaveInstance](https://github.com/Arasthel/SwissKnife/wiki/@SaveInstance)
* [@Parcelable](https://github.com/Arasthel/SwissKnife/wiki/@Parcelable)
* [@Extra](https://github.com/Arasthel/SwissKnife/wiki/@Extra)

I would also like to thank **[@Dexafree](https://github.com/dexafree)** for his help writing the wiki, testing the library and creating the sample app, which contains some usage examples.

You can find a all the releases and their changes here: [RELEASES](https://github.com/OrdonTeam/SwissKnife/releases)

## Using it

To use SwissKnife **you must use Groovy on your Android project** as the code generation is done using AST processing, which is a Groovy feature. You can learn how to do that using [this plugin](https://github.com/melix/groovy-android-gradle-plugin) on the [wiki pages](https://github.com/Arasthel/SwissKnife/wiki/How-to-use-Groovy).

Once your project App Module is configured to use Groovy you can add this library as a dependency cloning it with ``git clone`` or as a maven library on the **build.gradle** of your App Module:

```groovy
dependencies {
    ...
    compile "com.arasthel:swissknife:1.3.0"
    ...
}
```

If you want *SwissKnife* to update automatically, you could just type:

```groovy
dependencies {
    ...
    compile "com.arasthel:swissknife:+""
    ...
}
```

Also, [there is an IntelliJ IDEA plugin](https://github.com/Arasthel/SwissKnife/wiki/SwissKnife-IDEA-Plugin) *compatible with Android Studio* that lets you auto-generate the annotations and compatible method declarations.

![IDEA plugin](https://camo.githubusercontent.com/ffe3a4e6c05f0846162e93ed4d8abfd532b7f826/687474703a2f2f692e696d6775722e636f6d2f5564704e3634652e6a7067)

## License

SwissKnife is licensed under Apache v2 License, which means that it is Open Source and free to use and modify.

```
Copyright 2014 Jorge Martín Espinosa (Arasthel)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
