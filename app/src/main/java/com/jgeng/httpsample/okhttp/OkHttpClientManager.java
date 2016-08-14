package com.jgeng.httpsample.okhttp;

/**
 * Created by jgeng on 8/12/16.
 */
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import com.google.gson.Gson;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import com.google.gson.internal.$Gson$Types;

public class OkHttpClientManager {
  private static OkHttpClientManager mInstance;
  private OkHttpClient mOkHttpClient;
  private Handler mDelivery;
  private Gson mGson;

  public static OkHttpClientManager getInstance()
  {
    if (mInstance == null)
    {
      synchronized (OkHttpClientManager.class)
      {
        if (mInstance == null)
        {
          mInstance = new OkHttpClientManager();
        }
      }
    }
    return mInstance;
  }

  public static Response getSync(String url) throws IOException {
    return mInstance._getSyn(url);
  }

  public void getAsyn(String url, final ResultCallback callback) {
    mInstance._getAsyn(url, callback);
  }

  private OkHttpClientManager()
  {
    mOkHttpClient = new OkHttpClient();
    //cookie enabled
    mOkHttpClient.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
    mDelivery = new Handler(Looper.getMainLooper());
    mGson = new Gson();
  }


  /**
   *
   * @param url
   * @return Response
   */
  private Response _getSyn(String url) throws IOException
  {
    final Request request = new Request.Builder()
        .url(url)
        .build();
    Call call = mOkHttpClient.newCall(request);
    Response execute = call.execute();
    return execute;
  }

  /**
   * 异步的get请求
   *
   * @param url
   * @param callback
   */
  private void _getAsyn(String url, final ResultCallback callback)
  {
    final Request request = new Request.Builder()
        .url(url)
        .build();
    deliveryResult(callback, request);
  }


  private void sendFailedStringCallback(final Request request, final Exception e, final ResultCallback callback)
  {
    mDelivery.post(new Runnable()
    {
      @Override
      public void run()
      {
        if (callback != null)
          callback.onError(request, e);
      }
    });
  }

  private void sendSuccessResultCallback(final Object object, final ResultCallback callback)
  {
    mDelivery.post(new Runnable()
    {
      @Override
      public void run()
      {
        if (callback != null)
        {
          callback.onResponse(object);
        }
      }
    });
  }

  private void deliveryResult(final ResultCallback callback, Request request)
  {
    mOkHttpClient.newCall(request).enqueue(new Callback()
    {
      @Override
      public void onFailure(final Request request, final IOException e)
      {
        sendFailedStringCallback(request, e, callback);
      }

      @Override
      public void onResponse(final Response response)
      {
        try
        {
          final String string = response.body().string();
          if (callback.mType == String.class)
          {
            sendSuccessResultCallback(string, callback);
          } else
          {
            Object o = mGson.fromJson(string, callback.mType);
            sendSuccessResultCallback(o, callback);
          }


        } catch (IOException e)
        {
          sendFailedStringCallback(response.request(), e, callback);
        } catch (com.google.gson.JsonParseException e)//Json解析的错误
        {
          sendFailedStringCallback(response.request(), e, callback);
        }

      }
    });
  }

  private static final String SESSION_KEY = "Set-Cookie";
  private static final String mSessionKey = "JSESSIONID";

  private Map<String, String> mSessions = new HashMap<String, String>();

  public static abstract class ResultCallback<T>
  {
    Type mType;

    public ResultCallback()
    {
      mType = getSuperclassTypeParameter(getClass());
    }

    static Type getSuperclassTypeParameter(Class<?> subclass)
    {
      Type superclass = subclass.getGenericSuperclass();
      if (superclass instanceof Class)
      {
        throw new RuntimeException("Missing type parameter.");
      }
      ParameterizedType parameterized = (ParameterizedType) superclass;
      return $Gson$Types.canonicalize(parameterized.getActualTypeArguments()[0]);
    }

    public abstract void onError(Request request, Exception e);

    public abstract void onResponse(T response);
  }
}
