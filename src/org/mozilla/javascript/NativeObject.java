/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;

/**
 * This class implements the Object native object.
 * See ECMA 15.2.
 * @author Norris Boyd
 */
public class NativeObject extends IdScriptableObject implements Map
{
    private static final long serialVersionUID = -6345305608474346996L;

    private static final Object OBJECT_TAG = "Object";

    static void init(Scriptable scope, boolean sealed)
    {
        NativeObject obj = new NativeObject();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    public String getClassName()
    {
        return "Object";
    }

    @Override
    public String toString()
    {
        return ScriptRuntime.defaultObjectToString(this);
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor)
    {
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_getPrototypeOf,
                "getPrototypeOf", 1);
        if (Context.getCurrentContext().version >= Context.VERSION_ES6) {
            addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_setPrototypeOf,
                    "setPrototypeOf", 2);
        }
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_keys,
                "keys", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_getOwnPropertyNames,
                "getOwnPropertyNames", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_getOwnPropertySymbols,
                "getOwnPropertySymbols", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_getOwnPropertyDescriptor,
                "getOwnPropertyDescriptor", 2);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_defineProperty,
                "defineProperty", 3);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_isExtensible,
                "isExtensible", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_preventExtensions,
                "preventExtensions", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_defineProperties,
                "defineProperties", 2);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_create,
                "create", 2);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_isSealed,
                "isSealed", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_isFrozen,
                "isFrozen", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_seal,
                "seal", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_freeze,
                "freeze", 1);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_assign,
                "assign", 2);
        addIdFunctionProperty(ctor, OBJECT_TAG, ConstructorId_is,
                "is", 2);
        super.fillConstructorProperties(ctor);
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
          case Id_constructor:    arity=1; s="constructor";    break;
          case Id_toString:       arity=0; s="toString";       break;
          case Id_toLocaleString: arity=0; s="toLocaleString"; break;
          case Id_valueOf:        arity=0; s="valueOf";        break;
          case Id_hasOwnProperty: arity=1; s="hasOwnProperty"; break;
          case Id_propertyIsEnumerable:
            arity=1; s="propertyIsEnumerable"; break;
          case Id_isPrototypeOf:  arity=1; s="isPrototypeOf";  break;
          case Id_toSource:       arity=0; s="toSource";       break;
          case Id___defineGetter__:
            arity=2; s="__defineGetter__";     break;
          case Id___defineSetter__:
            arity=2; s="__defineSetter__";     break;
          case Id___lookupGetter__:
            arity=1; s="__lookupGetter__";     break;
          case Id___lookupSetter__:
            arity=1; s="__lookupSetter__";     break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(OBJECT_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(OBJECT_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
          case Id_constructor: {
            if (thisObj != null) {
                // BaseFunction.construct will set up parent, proto
                return f.construct(cx, scope, args);
            }
            if (args.length == 0
                    || args[0] == null
                    || Undefined.isUndefined(args[0]))
            {
                return new NativeObject();
            }
            return ScriptRuntime.toObject(cx, scope, args[0]);
          }

          case Id_toLocaleString: {
            Object toString = ScriptableObject.getProperty(thisObj, "toString");
            if(!(toString instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(toString);
            }
            Callable fun = (Callable)toString;
            return fun.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
          }

          case Id_toString: {
            if (cx.hasFeature(Context.FEATURE_TO_STRING_AS_SOURCE)) {
                String s = ScriptRuntime.defaultObjectToSource(cx, scope,
                                                               thisObj, args);
                int L = s.length();
                if (L != 0 && s.charAt(0) == '(' && s.charAt(L - 1) == ')') {
                    // Strip () that surrounds toSource
                    s = s.substring(1, L - 1);
                }
                return s;
            }
            return ScriptRuntime.defaultObjectToString(thisObj);
          }

          case Id_valueOf:
              if (cx.getLanguageVersion() >= Context.VERSION_1_8 && (thisObj == null || Undefined.isUndefined(thisObj))) {
                  throw ScriptRuntime.typeErrorById("msg." + (thisObj == null ? "null" : "undef") + ".to.object");
              }
            return thisObj;

          case Id_hasOwnProperty: {
              if (cx.getLanguageVersion() >= Context.VERSION_1_8 && (thisObj == null || Undefined.isUndefined(thisObj))) {
                  throw ScriptRuntime.typeErrorById("msg." + (thisObj == null ? "null" : "undef") + ".to.object");
              }
              boolean result;
              Object arg = args.length < 1 ? Undefined.instance : args[0];
              if (arg instanceof Symbol) {
                  result = ensureSymbolScriptable(thisObj).has((Symbol) arg, thisObj);
              } else {
                  StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, arg);
                  if (s.stringId == null) {
                      result = thisObj.has(s.index, thisObj);
                  } else {
                      result = thisObj.has(s.stringId, thisObj);
                  }
              }
              return ScriptRuntime.wrapBoolean(result);
          }

          case Id_propertyIsEnumerable: {
              if (cx.getLanguageVersion() >= Context.VERSION_1_8 && (thisObj == null || Undefined.isUndefined(thisObj))) {
                  throw ScriptRuntime.typeErrorById("msg." + (thisObj == null ? "null" : "undef") + ".to.object");
              }

            boolean result;
            Object arg = args.length < 1 ? Undefined.instance : args[0];

            if (arg instanceof Symbol) {
                result = ((SymbolScriptable)thisObj).has((Symbol)arg, thisObj);
                if (result && thisObj instanceof ScriptableObject) {
                    ScriptableObject so = (ScriptableObject)thisObj;
                    int attrs = so.getAttributes((Symbol)arg);
                    result = ((attrs & ScriptableObject.DONTENUM) == 0);
                }
            } else {
                StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, arg);
                // When checking if a property is enumerable, a missing property should return "false" instead of
                // throwing an exception.  See: https://github.com/mozilla/rhino/issues/415
                try {
                    if (s.stringId == null) {
                        result = thisObj.has(s.index, thisObj);
                        if (result && thisObj instanceof ScriptableObject) {
                            ScriptableObject so = (ScriptableObject) thisObj;
                            int attrs = so.getAttributes(s.index);
                            result = ((attrs & ScriptableObject.DONTENUM) == 0);
                        }
                    } else {
                        result = thisObj.has(s.stringId, thisObj);
                        if (result && thisObj instanceof ScriptableObject) {
                            ScriptableObject so = (ScriptableObject) thisObj;
                            int attrs = so.getAttributes(s.stringId);
                            result = ((attrs & ScriptableObject.DONTENUM) == 0);
                        }
                    }
                } catch (EvaluatorException ee) {
                    if (ee.getMessage().startsWith(ScriptRuntime.getMessageById("msg.prop.not.found",
                                                        s.stringId == null ? Integer.toString(s.index) : s.stringId))) {
                        result = false;
                    } else {
                        throw ee;
                    }
                }
            }
            return ScriptRuntime.wrapBoolean(result);
          }

          case Id_isPrototypeOf: {
            if (cx.getLanguageVersion() >= Context.VERSION_1_8 && (thisObj == null || Undefined.isUndefined(thisObj))) {
                throw ScriptRuntime.typeErrorById("msg." + (thisObj == null ? "null" : "undef") + ".to.object");
            }

            boolean result = false;
            if (args.length != 0 && args[0] instanceof Scriptable) {
                Scriptable v = (Scriptable) args[0];
                do {
                    v = v.getPrototype();
                    if (v == thisObj) {
                        result = true;
                        break;
                    }
                } while (v != null);
            }
            return ScriptRuntime.wrapBoolean(result);
          }

          case Id_toSource:
            return ScriptRuntime.defaultObjectToSource(cx, scope, thisObj,
                                                       args);
          case Id___defineGetter__:
          case Id___defineSetter__:
            {
                if (args.length < 2 || !(args[1] instanceof Callable)) {
                    Object badArg = (args.length >= 2 ? args[1] : Undefined.instance);
                    throw ScriptRuntime.notFunctionError(badArg);
                }
                if (!(thisObj instanceof ScriptableObject)) {
                    throw Context.reportRuntimeErrorById(
                        "msg.extend.scriptable",
                        thisObj == null ? "null" : thisObj.getClass().getName(),
                        String.valueOf(args[0]));
                }
                ScriptableObject so = (ScriptableObject)thisObj;
                StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
                int index = s.stringId != null ? 0 : s.index;
                Callable getterOrSetter = (Callable)args[1];
                boolean isSetter = (id == Id___defineSetter__);
                so.setGetterOrSetter(s.stringId, index, getterOrSetter, isSetter);
                if (so instanceof NativeArray)
                    ((NativeArray)so).setDenseOnly(false);
            }
            return Undefined.instance;

            case Id___lookupGetter__:
            case Id___lookupSetter__:
              {
                  if (args.length < 1 ||
                      !(thisObj instanceof ScriptableObject))
                      return Undefined.instance;

                  ScriptableObject so = (ScriptableObject)thisObj;
                  StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
                  int index = s.stringId != null ? 0 : s.index;
                  boolean isSetter = (id == Id___lookupSetter__);
                  Object gs;
                  for (;;) {
                      gs = so.getGetterOrSetter(s.stringId, index, isSetter);
                      if (gs != null)
                          break;
                      // If there is no getter or setter for the object itself,
                      // how about the prototype?
                      Scriptable v = so.getPrototype();
                      if (v == null)
                          break;
                      if (v instanceof ScriptableObject)
                          so = (ScriptableObject)v;
                      else
                          break;
                  }
                  if (gs != null) {
                      if (gs instanceof MemberBox) {
                          if (isSetter) {
                              gs = ((MemberBox) gs).asSetterFunction(s.stringId, this);
                          } else {
                              gs = ((MemberBox) gs).asGetterFunction(s.stringId, this);
                          }
                      }
                      return gs;
                  }
              }
              return Undefined.instance;

          case ConstructorId_getPrototypeOf:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                Scriptable obj = getCompatibleObject(cx, scope, arg);
                return obj.getPrototype();
              }
          case ConstructorId_setPrototypeOf:
              {
                if (args.length < 2) {
                    throw ScriptRuntime.typeErrorById("msg.method.missing.parameter", "Object.setPrototypeOf", "2", Integer.toString(args.length));
                }
                Scriptable proto = (args[1] == null) ? null : ensureScriptable(args[1]);
                if (proto instanceof Symbol) {
                    throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(proto));
                }

                final Object arg0 = args[0];
                if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
                    ScriptRuntimeES6.requireObjectCoercible(cx, arg0, f);
                }
                if ( !(arg0 instanceof ScriptableObject) ) {
                    return arg0;
                }
                ScriptableObject obj = (ScriptableObject) arg0;
                if (!obj.isExtensible()) {
                    throw ScriptRuntime.typeErrorById("msg.not.extensible");
                }

                // cycle detection
                Scriptable prototypeProto = proto;
                while (prototypeProto != null) {
                    if (prototypeProto == obj) {
                        throw ScriptRuntime.typeErrorById("msg.object.cyclic.prototype", obj.getClass().getSimpleName());
                    }
                    prototypeProto = prototypeProto.getPrototype();
                }
                obj.setPrototype(proto);
                return obj;
              }
          case ConstructorId_keys:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                Scriptable obj = getCompatibleObject(cx, scope, arg);
                Object[] ids = obj.getIds();
                for (int i = 0; i < ids.length; i++) {
                  ids[i] = ScriptRuntime.toString(ids[i]);
                }
                return cx.newArray(scope, ids);
              }
          case ConstructorId_getOwnPropertyNames:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                Scriptable s = getCompatibleObject(cx, scope, arg);
                ScriptableObject obj = ensureScriptableObject(s);
                Object[] ids = obj.getIds(true, false);
                for (int i = 0; i < ids.length; i++) {
                  ids[i] = ScriptRuntime.toString(ids[i]);
                }
                return cx.newArray(scope, ids);
              }
            case ConstructorId_getOwnPropertySymbols:
            {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                Scriptable s = getCompatibleObject(cx, scope, arg);
                ScriptableObject obj = ensureScriptableObject(s);
                Object[] ids = obj.getIds(true, true);
                ArrayList<Object> syms = new ArrayList<Object>();
                for (int i = 0; i < ids.length; i++) {
                    if (ids[i] instanceof Symbol) {
                        syms.add(ids[i]);
                    }
                }
                return cx.newArray(scope, syms.toArray());
            }
          case ConstructorId_getOwnPropertyDescriptor:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                // TODO(norris): There's a deeper issue here if
                // arg instanceof Scriptable. Should we create a new
                // interface to admit the new ECMAScript 5 operations?
                Scriptable s = getCompatibleObject(cx, scope, arg);
                ScriptableObject obj = ensureScriptableObject(s);
                Object nameArg = args.length < 2 ? Undefined.instance : args[1];
                Scriptable desc = obj.getOwnPropertyDescriptor(cx, nameArg);
                return desc == null ? Undefined.instance : desc;
              }
          case ConstructorId_defineProperty:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                ScriptableObject obj = ensureScriptableObject(arg);
                Object name = args.length < 2 ? Undefined.instance : args[1];
                Object descArg = args.length < 3 ? Undefined.instance : args[2];
                ScriptableObject desc = ensureScriptableObject(descArg);
                obj.defineOwnProperty(cx, name, desc);
                return obj;
              }
          case ConstructorId_isExtensible:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                if (cx.getLanguageVersion() >= Context.VERSION_ES6
                        && !(arg instanceof ScriptableObject)) {
                  return Boolean.FALSE;
                }

                ScriptableObject obj = ensureScriptableObject(arg);
                return Boolean.valueOf(obj.isExtensible());
              }
          case ConstructorId_preventExtensions:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                if (cx.getLanguageVersion() >= Context.VERSION_ES6
                        && !(arg instanceof ScriptableObject)) {
                  return arg;
                }

                ScriptableObject obj = ensureScriptableObject(arg);
                obj.preventExtensions();
                return obj;
              }
          case ConstructorId_defineProperties:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                ScriptableObject obj = ensureScriptableObject(arg);
                Object propsObj = args.length < 2 ? Undefined.instance : args[1];
                Scriptable props = Context.toObject(propsObj, scope);
                obj.defineOwnProperties(cx, ensureScriptableObject(props));
                return obj;
              }
          case ConstructorId_create:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                Scriptable obj = (arg == null) ? null : ensureScriptable(arg);

                ScriptableObject newObject = new NativeObject();
                newObject.setParentScope(scope);
                newObject.setPrototype(obj);

                if (args.length > 1 && !Undefined.isUndefined(args[1])) {
                  Scriptable props = Context.toObject(args[1], scope);
                  newObject.defineOwnProperties(cx, ensureScriptableObject(props));
                }

                return newObject;
              }
          case ConstructorId_isSealed:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                if (cx.getLanguageVersion() >= Context.VERSION_ES6
                        && !(arg instanceof ScriptableObject)) {
                  return Boolean.TRUE;
                }

                ScriptableObject obj = ensureScriptableObject(arg);

                if (obj.isExtensible()) return Boolean.FALSE;

                for (Object name: obj.getAllIds()) {
                  Object configurable = obj.getOwnPropertyDescriptor(cx, name).get("configurable");
                  if (Boolean.TRUE.equals(configurable))
                    return Boolean.FALSE;
                }

                return Boolean.TRUE;
              }
          case ConstructorId_isFrozen:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                if (cx.getLanguageVersion() >= Context.VERSION_ES6
                        && !(arg instanceof ScriptableObject)) {
                  return Boolean.TRUE;
                }

                ScriptableObject obj = ensureScriptableObject(arg);

                if (obj.isExtensible()) return Boolean.FALSE;

                for (Object name: obj.getAllIds()) {
                  ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
                  if (Boolean.TRUE.equals(desc.get("configurable")))
                    return Boolean.FALSE;
                  if (isDataDescriptor(desc) && Boolean.TRUE.equals(desc.get("writable")))
                    return Boolean.FALSE;
                }

                return Boolean.TRUE;
              }
          case ConstructorId_seal:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                if (cx.getLanguageVersion() >= Context.VERSION_ES6
                        && !(arg instanceof ScriptableObject)) {
                  return arg;
                }

                ScriptableObject obj = ensureScriptableObject(arg);

                for (Object name: obj.getAllIds()) {
                  ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
                  if (Boolean.TRUE.equals(desc.get("configurable"))) {
                    desc.put("configurable", desc, Boolean.FALSE);
                    obj.defineOwnProperty(cx, name, desc, false);
                  }
                }
                obj.preventExtensions();

                return obj;
              }
          case ConstructorId_freeze:
              {
                Object arg = args.length < 1 ? Undefined.instance : args[0];
                if (cx.getLanguageVersion() >= Context.VERSION_ES6
                      && !(arg instanceof ScriptableObject)) {
                  return arg;
                }

                ScriptableObject obj = ensureScriptableObject(arg);

                for (Object name: obj.getIds(true, true)) {
                  ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, name);
                  if (isDataDescriptor(desc) && Boolean.TRUE.equals(desc.get("writable"))) {
                    desc.put("writable", desc, Boolean.FALSE);
                  }
                  if (Boolean.TRUE.equals(desc.get("configurable"))) {
                    desc.put("configurable", desc, Boolean.FALSE);
                  }
                  obj.defineOwnProperty(cx, name, desc, false);
                }
                obj.preventExtensions();

                return obj;
              }

          case ConstructorId_assign:
          {
            Scriptable targetObj;
            if (args.length > 0) {
              targetObj = ScriptRuntime.toObject(cx, scope, args[0]);
            }
            else {
              targetObj = ScriptRuntime.toObject(cx, scope, Undefined.instance);
            }
            for (int i = 1; i < args.length; i++) {
              if ((args[i] == null) || Undefined.isUndefined(args[i])) {
                continue;
              }
              Scriptable sourceObj = ScriptRuntime.toObject(cx, scope, args[i]);
              Object[] ids = sourceObj.getIds();
              for (Object key : ids) {
                if (targetObj instanceof ScriptableObject) {
                  ScriptableObject desc = ((ScriptableObject) targetObj).getOwnPropertyDescriptor(cx, key);
                  if (desc != null && isDataDescriptor(desc) && isFalse(desc.get("writable"))) {
                      throw ScriptRuntime.typeErrorById("msg.change.value.with.writable.false", key);
                  }
                }
                if (key instanceof String) {
                  Object val = sourceObj.get((String) key, sourceObj);
                  if ((val != Scriptable.NOT_FOUND) && !Undefined.isUndefined(val)) {
                    targetObj.put((String) key, targetObj, val);
                  }
                } else if (key instanceof Number) {
                  int ii = ScriptRuntime.toInt32(key);
                  Object val = sourceObj.get(ii, sourceObj);
                  if ((val != Scriptable.NOT_FOUND) && !Undefined.isUndefined(val)) {
                    targetObj.put(ii, targetObj, val);
                  }
                }
              }
            }
            return targetObj;
          }

          case ConstructorId_is:
          {
            Object a1 = args.length < 1 ? Undefined.instance : args[0];
            Object a2 = args.length < 2 ? Undefined.instance : args[1];
            return ScriptRuntime.wrapBoolean(ScriptRuntime.same(a1, a2));
          }


          default:
            throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    private static Scriptable getCompatibleObject(Context cx, Scriptable scope, Object arg)
    {
        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            Scriptable s = ScriptRuntime.toObject(cx, scope, arg);
            return ensureScriptable(s);
        }
        return ensureScriptable(arg);
    }

    // methods implementing java.util.Map

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return has((String) key, this);
        } else if (key instanceof Number) {
            return has(((Number) key).intValue(), this);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Object obj : values()) {
            if (value == obj ||
                    value != null && value.equals(obj)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object remove(Object key) {
        Object value = get(key);
        if (key instanceof String) {
            delete((String) key);
        } else if (key instanceof Number) {
            delete(((Number) key).intValue());
        }
        return value;
    }

    @Override
    public Set<Object> keySet() {
        return new KeySet();
    }

    @Override
    public Collection<Object> values() {
        return new ValueCollection();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return new EntrySet();
    }

    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }


    class EntrySet extends AbstractSet<Entry<Object, Object>> {
        @Override
        public Iterator<Entry<Object, Object>> iterator() {
            return new Iterator<Map.Entry<Object, Object>>() {
                Object[] ids = getIds();
                Object key = null;
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < ids.length;
                }

                @Override
                public Map.Entry<Object, Object> next() {
                    final Object ekey = key = ids[index++];
                    final Object value = get(key);
                    return new Map.Entry<Object, Object>() {
                        @Override
                        public Object getKey() {
                            return ekey;
                        }

                        @Override
                        public Object getValue() {
                            return value;
                        }

                        @Override
                        public Object setValue(Object value) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (!(other instanceof Map.Entry)) {
                                return false;
                            }
                            Map.Entry<?, ?> e = (Map.Entry<?, ?>) other;
                            return (ekey == null ? e.getKey() == null : ekey.equals(e.getKey()))
                                && (value == null ? e.getValue() == null : value.equals(e.getValue()));
                        }

                        @Override
                        public int hashCode() {
                            return (ekey == null ? 0 : ekey.hashCode()) ^
                                   (value == null ? 0 : value.hashCode());
                        }

                        @Override
                        public String toString() {
                            return ekey + "=" + value;
                        }
                    };
                }

                @Override
                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
            };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }

    class KeySet extends AbstractSet<Object> {

        @Override
        public boolean contains(Object key) {
            return containsKey(key);
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                Object[] ids = getIds();
                Object key;
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < ids.length;
                }

                @Override
                public Object next() {
                    try {
                        return (key = ids[index++]);
                    } catch(ArrayIndexOutOfBoundsException e) {
                        key = null;
                        throw new NoSuchElementException();
                    }
                }

                @Override
                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
           };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }

    class ValueCollection extends AbstractCollection<Object> {

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                Object[] ids = getIds();
                Object key;
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < ids.length;
                }

                @Override
                public Object next() {
                    return get((key = ids[index++]));
                }

                @Override
                public void remove() {
                    if (key == null) {
                        throw new IllegalStateException();
                    }
                    NativeObject.this.remove(key);
                    key = null;
                }
            };
        }

        @Override
        public int size() {
            return NativeObject.this.size();
        }
    }


// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2021-03-21 09:51:05 MEZ
        switch (s) {
        case "constructor":
            id = Id_constructor;
            break;
        case "toString":
            id = Id_toString;
            break;
        case "toLocaleString":
            id = Id_toLocaleString;
            break;
        case "valueOf":
            id = Id_valueOf;
            break;
        case "hasOwnProperty":
            id = Id_hasOwnProperty;
            break;
        case "propertyIsEnumerable":
            id = Id_propertyIsEnumerable;
            break;
        case "isPrototypeOf":
            id = Id_isPrototypeOf;
            break;
        case "toSource":
            id = Id_toSource;
            break;
        case "__defineGetter__":
            id = Id___defineGetter__;
            break;
        case "__defineSetter__":
            id = Id___defineSetter__;
            break;
        case "__lookupGetter__":
            id = Id___lookupGetter__;
            break;
        case "__lookupSetter__":
            id = Id___lookupSetter__;
            break;
        default:
            id = 0;
            break;
        }
// #/generated#
        return id;
    }

    private static final int
        ConstructorId_getPrototypeOf = -1,
        ConstructorId_keys = -2,
        ConstructorId_getOwnPropertyNames = -3,
        ConstructorId_getOwnPropertyDescriptor = -4,
        ConstructorId_defineProperty = -5,
        ConstructorId_isExtensible = -6,
        ConstructorId_preventExtensions = -7,
        ConstructorId_defineProperties= -8,
        ConstructorId_create = -9,
        ConstructorId_isSealed = -10,
        ConstructorId_isFrozen = -11,
        ConstructorId_seal = -12,
        ConstructorId_freeze = -13,
        ConstructorId_getOwnPropertySymbols = -14,
        ConstructorId_assign = -15,
        ConstructorId_is = -16,
        ConstructorId_setPrototypeOf = -17,

        Id_constructor           = 1,
        Id_toString              = 2,
        Id_toLocaleString        = 3,
        Id_valueOf               = 4,
        Id_hasOwnProperty        = 5,
        Id_propertyIsEnumerable  = 6,
        Id_isPrototypeOf         = 7,
        Id_toSource              = 8,
        Id___defineGetter__      = 9,
        Id___defineSetter__      = 10,
        Id___lookupGetter__      = 11,
        Id___lookupSetter__      = 12,
        MAX_PROTOTYPE_ID         = 12;

// #/string_id_map#
}
