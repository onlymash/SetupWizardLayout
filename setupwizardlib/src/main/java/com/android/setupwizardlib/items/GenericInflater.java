/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.setupwizardlib.items;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.InflateException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * Generic XML inflater. This class is modeled after {@code android.preference.GenericInflater},
 * which is in turn modeled after {@code LayoutInflater}. This can be used to recursively inflate a
 * hierarchy of items. All items in the hierarchy must inherit the generic type {@code T}, and the
 * specific implementation is expected to handle inserting child items into the parent, by
 * implementing {@link #onAddChildItem(Object, Object)}.
 *
 * @param <T> Type of the items to inflate
 */
public abstract class GenericInflater<T> {

    private static final String TAG = "GenericInflater";
    private static final boolean DEBUG = false;
    private static final Class[] mConstructorSignature = new Class[]{
            Context.class, AttributeSet.class};
    private static final HashMap<String, Constructor<?>> sConstructorMap = new HashMap<>();
    protected final Context mContext;
    private final Object[] mConstructorArgs = new Object[2];
    // these are optional, set by the caller
    private boolean mFactorySet;
    private Factory<T> mFactory;
    private String mDefaultPackage;

    /**
     * Create a new inflater instance associated with a
     * particular Context.
     *
     * @param context The Context in which this inflater will
     *                create its items; most importantly, this supplies the theme
     *                from which the default values for their attributes are
     *                retrieved.
     */
    protected GenericInflater(Context context) {
        mContext = context;
    }

    /**
     * Create a new inflater instance that is a copy of an
     * existing inflater, optionally with its Context
     * changed. For use in implementing {@link #cloneInContext}.
     *
     * @param original   The original inflater to copy.
     * @param newContext The new Context to use.
     */
    protected GenericInflater(GenericInflater<T> original, Context newContext) {
        mContext = newContext;
        mFactory = original.mFactory;
    }

    /**
     * Create a copy of the existing inflater object, with the copy
     * pointing to a different Context than the original.  This is used by
     * {@link ContextThemeWrapper} to create a new inflater to go along
     * with the new Context theme.
     *
     * @param newContext The new Context to associate with the new inflater.
     *                   May be the same as the original Context if desired.
     * @return Returns a brand spanking new inflater object associated with
     * the given Context.
     */
    public abstract GenericInflater cloneInContext(Context newContext);

    /**
     * Returns the default package, or null if it is not set.
     *
     * @return The default package.
     * @see #setDefaultPackage(String)
     */
    public String getDefaultPackage() {
        return mDefaultPackage;
    }

    /**
     * Sets the default package that will be searched for classes to construct
     * for tag names that have no explicit package.
     *
     * @param defaultPackage The default package. This will be prepended to the
     *                       tag name, so it should end with a period.
     */
    public void setDefaultPackage(String defaultPackage) {
        mDefaultPackage = defaultPackage;
    }

    /**
     * Return the context we are running in, for access to resources, class
     * loader, etc.
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Return the current factory (or null). This is called on each element
     * name. If the factory returns an item, add that to the hierarchy. If it
     * returns null, proceed to call onCreateItem(name).
     */
    public final Factory<T> getFactory() {
        return mFactory;
    }

    /**
     * Attach a custom Factory interface for creating items while using this
     * inflater. This must not be null, and can only be set
     * once; after setting, you can not change the factory. This is called on
     * each element name as the XML is parsed. If the factory returns an item,
     * that is added to the hierarchy. If it returns null, the next factory
     * default {@link #onCreateItem} method is called.
     * <p>
     * If you have an existing inflater and want to add your
     * own factory to it, use {@link #cloneInContext} to clone the existing
     * instance and then you can use this function (once) on the returned new
     * instance. This will merge your own factory with whatever factory the
     * original instance is using.
     */
    public void setFactory(Factory<T> factory) {
        if (mFactorySet) {
            throw new IllegalStateException("" +
                    "A factory has already been set on this inflater");
        }
        if (factory == null) {
            throw new NullPointerException("Given factory can not be null");
        }
        mFactorySet = true;
        if (mFactory == null) {
            mFactory = factory;
        } else {
            mFactory = new FactoryMerger<>(factory, mFactory);
        }
    }

    public T inflate(int resource) {
        return inflate(resource, null);
    }

    /**
     * Inflate a new item hierarchy from the specified xml resource. Throws
     * InflaterException if there is an error.
     *
     * @param resource ID for an XML resource to load (e.g.,
     *                 <code>R.layout.main_page</code>)
     * @param root     Optional parent of the generated hierarchy.
     * @return The root of the inflated hierarchy. If root was supplied,
     * this is the root item; otherwise it is the root of the inflated
     * XML file.
     */
    public T inflate(int resource, T root) {
        return inflate(resource, root, root != null);
    }

    /**
     * Inflate a new hierarchy from the specified xml node. Throws
     * InflaterException if there is an error. *
     * <p>
     * <em><strong>Important</strong></em>&nbsp;&nbsp;&nbsp;For performance
     * reasons, inflation relies heavily on pre-processing of XML files
     * that is done at build time. Therefore, it is not currently possible to
     * use inflater with an XmlPullParser over a plain XML file at runtime.
     *
     * @param parser XML dom node containing the description of the
     *               hierarchy.
     * @param root   Optional parent of the generated hierarchy.
     * @return The root of the inflated hierarchy. If root was supplied,
     * this is the that; otherwise it is the root of the inflated
     * XML file.
     */
    public T inflate(XmlPullParser parser, T root) {
        return inflate(parser, root, root != null);
    }

    /**
     * Inflate a new hierarchy from the specified xml resource. Throws
     * InflaterException if there is an error.
     *
     * @param resource     ID for an XML resource to load (e.g.,
     *                     <code>R.layout.main_page</code>)
     * @param root         Optional root to be the parent of the generated hierarchy (if
     *                     <em>attachToRoot</em> is true), or else simply an object that
     *                     provides a set of values for root of the returned
     *                     hierarchy (if <em>attachToRoot</em> is false.)
     * @param attachToRoot Whether the inflated hierarchy should be attached to
     *                     the root parameter?
     * @return The root of the inflated hierarchy. If root was supplied and
     * attachToRoot is true, this is root; otherwise it is the root of
     * the inflated XML file.
     */
    public T inflate(int resource, T root, boolean attachToRoot) {
        if (DEBUG)
            Log.v(TAG, "INFLATING from resource: " + resource);
        XmlResourceParser parser = getContext().getResources().getXml(resource);
        try {
            return inflate(parser, root, attachToRoot);
        } finally {
            parser.close();
        }
    }

    /**
     * Inflate a new hierarchy from the specified XML node. Throws
     * InflaterException if there is an error.
     * <p>
     * <em><strong>Important</strong></em>&nbsp;&nbsp;&nbsp;For performance
     * reasons, inflation relies heavily on pre-processing of XML files
     * that is done at build time. Therefore, it is not currently possible to
     * use inflater with an XmlPullParser over a plain XML file at runtime.
     *
     * @param parser       XML dom node containing the description of the
     *                     hierarchy.
     * @param root         Optional to be the parent of the generated hierarchy (if
     *                     <em>attachToRoot</em> is true), or else simply an object that
     *                     provides a set of values for root of the returned
     *                     hierarchy (if <em>attachToRoot</em> is false.)
     * @param attachToRoot Whether the inflated hierarchy should be attached to
     *                     the root parameter?
     * @return The root of the inflated hierarchy. If root was supplied and
     * attachToRoot is true, this is root; otherwise it is the root of
     * the inflated XML file.
     */
    public T inflate(XmlPullParser parser, T root, boolean attachToRoot) {
        synchronized (mConstructorArgs) {
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            mConstructorArgs[0] = mContext;
            T result;

            try {
                // Look for the root node.
                int type;
                while ((type = parser.next()) != XmlPullParser.START_TAG
                        && type != XmlPullParser.END_DOCUMENT) {
                }

                if (type != XmlPullParser.START_TAG) {
                    throw new InflateException(parser.getPositionDescription()
                            + ": No start tag found!");
                }

                if (DEBUG) {
                    Log.v(TAG, "**************************");
                    Log.v(TAG, "Creating root: "
                            + parser.getName());
                    Log.v(TAG, "**************************");
                }
                // Temp is the root that was found in the xml
                T xmlRoot = createItemFromTag(parser, parser.getName(), attrs);

                result = onMergeRoots(root, attachToRoot, xmlRoot);

                if (DEBUG)
                    Log.v(TAG, "-----> start inflating children");
                // Inflate all children under temp
                rInflate(parser, result, attrs);
                if (DEBUG)
                    Log.v(TAG, "-----> done inflating children");
            } catch (XmlPullParserException e) {
                InflateException ex = new InflateException(e.getMessage(), e);
                throw ex;
            } catch (IOException e) {
                InflateException ex = new InflateException(
                        parser.getPositionDescription()
                                + ": " + e.getMessage(), e);
                throw ex;
            }

            return result;
        }
    }

    /**
     * Low-level function for instantiating by name. This attempts to
     * instantiate class of the given <var>name</var> found in this
     * inflater's ClassLoader.
     * <p>
     * <p>
     * There are two things that can happen in an error case: either the
     * exception describing the error will be thrown, or a null will be
     * returned. You must deal with both possibilities -- the former will happen
     * the first time createItem() is called for a class of a particular name,
     * the latter every time there-after for that class name.
     *
     * @param name  The full name of the class to be instantiated.
     * @param attrs The XML attributes supplied for this instance.
     * @return The newly instantiated item, or null.
     */
    public final T createItem(String name, String prefix, AttributeSet attrs)
            throws ClassNotFoundException, InflateException {
        Constructor constructor = sConstructorMap.get(name);

        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real,
                // and try to add it
                Class<?> clazz = mContext.getClassLoader().loadClass(
                        prefix != null ? (prefix + name) : name);
                constructor = clazz.getConstructor(mConstructorSignature);
                constructor.setAccessible(true);
                sConstructorMap.put(name, constructor);
            }

            Object[] args = mConstructorArgs;
            args[1] = attrs;
            //noinspection unchecked
            return (T) constructor.newInstance(args);
        } catch (ClassNotFoundException e) {
            // If loadClass fails, we should propagate the exception.
            throw e;
        } catch (Exception e) {
            throw new InflateException(attrs.getPositionDescription()
                    + ": Error inflating class "
                    + (prefix != null ? (prefix + name) : name), e);

        }
    }

    /**
     * This routine is responsible for creating the correct subclass of item
     * given the xml element name. Override it to handle custom item objects. If
     * you override this in your subclass be sure to call through to
     * super.onCreateItem(name) for names you do not recognize.
     *
     * @param name  The fully qualified class name of the item to be create.
     * @param attrs An AttributeSet of attributes to apply to the item.
     * @return The item created.
     */
    protected T onCreateItem(String name, AttributeSet attrs) throws ClassNotFoundException {
        return createItem(name, mDefaultPackage, attrs);
    }

    private T createItemFromTag(XmlPullParser parser, String name, AttributeSet attrs) {
        if (DEBUG)
            Log.v(TAG, "******** Creating item: " + name);

        try {
            T item = (mFactory == null) ? null : mFactory.onCreateItem(name, mContext, attrs);

            if (item == null) {
                if (-1 == name.indexOf('.')) {
                    item = onCreateItem(name, attrs);
                } else {
                    item = createItem(name, null, attrs);
                }
            }

            if (DEBUG)
                Log.v(TAG, "Created item is: " + item);
            return item;

        } catch (InflateException e) {
            throw e;

        } catch (Exception e) {
            InflateException ie = new InflateException(attrs
                    .getPositionDescription()
                    + ": Error inflating class " + name, e);
            throw ie;
        }
    }

    /**
     * Recursive method used to descend down the xml hierarchy and instantiate
     * items, instantiate their children, and then call onFinishInflate().
     */
    private void rInflate(XmlPullParser parser, T node, final AttributeSet attrs)
            throws XmlPullParserException, IOException {
        final int depth = parser.getDepth();

        int type;
        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if (onCreateCustomFromTag(parser, node, attrs)) {
                continue;
            }

            if (DEBUG)
                Log.v(TAG, "Now inflating tag: " + parser.getName());
            String name = parser.getName();

            T item = createItemFromTag(parser, name, attrs);

            if (DEBUG)
                Log.v(TAG, "Creating params from parent: " + node);

            onAddChildItem(node, item);


            if (DEBUG)
                Log.v(TAG, "-----> start inflating children");
            rInflate(parser, item, attrs);
            if (DEBUG)
                Log.v(TAG, "-----> done inflating children");
        }
    }

    /**
     * Before this inflater tries to create an item from the tag, this method
     * will be called. The parser will be pointing to the start of a tag, you
     * must stop parsing and return when you reach the end of this element!
     *
     * @param parser XML dom node containing the description of the hierarchy.
     * @param node   The item that should be the parent of whatever you create.
     * @param attrs  An AttributeSet of attributes to apply to the item.
     * @return Whether you created a custom object (true), or whether this
     * inflater should proceed to create an item.
     */
    protected boolean onCreateCustomFromTag(XmlPullParser parser, T node,
                                            final AttributeSet attrs) throws XmlPullParserException {
        return false;
    }

    protected abstract void onAddChildItem(T parent, T child);

    protected T onMergeRoots(T givenRoot, boolean attachToGivenRoot, T xmlRoot) {
        return xmlRoot;
    }

    public interface Factory<T> {
        /**
         * Hook you can supply that is called when inflating from a
         * inflater. You can use this to customize the tag
         * names available in your XML files.
         * <p>
         * Note that it is good practice to prefix these custom names with your
         * package (i.e., com.coolcompany.apps) to avoid conflicts with system
         * names.
         *
         * @param name    Tag name to be inflated.
         * @param context The context the item is being created in.
         * @param attrs   Inflation attributes as specified in XML file.
         * @return Newly created item. Return null for the default behavior.
         */
        T onCreateItem(String name, Context context, AttributeSet attrs);
    }

    private static class FactoryMerger<T> implements Factory<T> {
        private final Factory<T> mF1, mF2;

        FactoryMerger(Factory<T> f1, Factory<T> f2) {
            mF1 = f1;
            mF2 = f2;
        }

        public T onCreateItem(String name, Context context, AttributeSet attrs) {
            T v = mF1.onCreateItem(name, context, attrs);
            if (v != null)
                return v;
            return mF2.onCreateItem(name, context, attrs);
        }
    }
}
