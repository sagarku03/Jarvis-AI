package com.jarvis.ai.ai

import org.json.JSONArray
import org.json.JSONObject

object JarvisToolRegistry {

    fun getToolsDeclaration(): JSONArray {
        val toolsArray = JSONArray()
        val functionDeclarations = JSONArray()

        // 1. open_app
        functionDeclarations.put(JSONObject().apply {
            put("name", "open_app")
            put("description", "Open any installed application on the phone like YouTube, WhatsApp, Camera, Settings, Chrome, etc.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("app_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the app to launch (e.g. 'YouTube', 'WhatsApp', 'Camera')")
                    })
                })
                put("required", JSONArray().apply { put("app_name") })
            })
        })

        // 2. make_call
        functionDeclarations.put(JSONObject().apply {
            put("name", "make_call")
            put("description", "Place a direct phone call to a phone number or contact.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("phone_number", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Phone number to call")
                    })
                })
                put("required", JSONArray().apply { put("phone_number") })
            })
        })

        // 3. set_alarm
        functionDeclarations.put(JSONObject().apply {
            put("name", "set_alarm")
            put("description", "Set an alarm on the phone for a specific hour and minute.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("hour", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Hour of the day in 24-hour format (0-23)")
                    })
                    put("minute", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Minute of the hour (0-59)")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Label / message for the alarm")
                    })
                })
                put("required", JSONArray().apply {
                    put("hour")
                    put("minute")
                })
            })
        })

        // 4. search_web
        functionDeclarations.put(JSONObject().apply {
            put("name", "search_web")
            put("description", "Search the web using Google Search when the user asks to look something up online.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search terms")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 5. send_whatsapp
        functionDeclarations.put(JSONObject().apply {
            put("name", "send_whatsapp")
            put("description", "Open WhatsApp chat with prefilled text message.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("phone_number", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional international phone number with country code")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Message text to prefill")
                    })
                })
                put("required", JSONArray().apply { put("message") })
            })
        })

        toolsArray.put(JSONObject().apply {
            put("functionDeclarations", functionDeclarations)
        })

        return toolsArray
    }
}
