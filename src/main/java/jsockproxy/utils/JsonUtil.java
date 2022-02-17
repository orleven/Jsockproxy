package jsockproxy.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JsonUtil {

	public static String toJSONString(Object o){
		if(o instanceof String) return (String)o;
		
		return JSON.toJSONString(o, 
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.WriteNullStringAsEmpty,
				SerializerFeature.WriteNullNumberAsZero,
				SerializerFeature.WriteNullBooleanAsFalse,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect
		);
	}
	
	public static JSONObject fromJson(String text) {
		return JSONObject.parseObject(text);
	}
	
	public static Object parseObject(String text, Class clazz){
		Object result = JSON.parseObject(text, clazz );
		return result;
	}

	public static void main(String[] args) {
		System.out.println("{\"name\":\"admin\"}");
		System.out.println(JsonUtil.toJSONString("{\"name\":\"admin\"}"));
	}
}
