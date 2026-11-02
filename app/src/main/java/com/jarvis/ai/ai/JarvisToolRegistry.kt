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
                        put("description", "Name of the app to launch (e.g. 'YouTube', 'WhatsApp', 'Camera', 'Instagram')")
                    })
                })
                put("required", JSONArray().apply { put("app_name") })
            })
        })

        // 2. call_contact
        functionDeclarations.put(JSONObject().apply {
            put("name", "call_contact")
            put("description", "Place a direct phone call to a contact name or phone number.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contact", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the contact or raw phone number to call")
                    })
                })
                put("required", JSONArray().apply { put("contact") })
            })
        })

        // 3. message_contact
        functionDeclarations.put(JSONObject().apply {
            put("name", "message_contact")
            put("description", "Prefill and prepare an SMS or WhatsApp message for a contact.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contact", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of contact or phone number")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Message text to send")
                    })
                    put("app", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'whatsapp' or 'sms' (default is 'sms')")
                    })
                })
                put("required", JSONArray().apply {
                    put("contact")
                    put("message")
                })
            })
        })

        // 4. search_contacts
        functionDeclarations.put(JSONObject().apply {
            put("name", "search_contacts")
            put("description", "Search contacts saved on the user's phone, find phone numbers, or list contacts.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional search name or leave empty to list all contacts")
                    })
                    put("limit", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Maximum contacts to return")
                    })
                })
            })
        })

        // 5. build_website (The Forge 3D Website Generator)
        functionDeclarations.put(JSONObject().apply {
            put("name", "build_website")
            put("description", "Build a complete animated website or web application using The Forge 3D Reactor engine.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("prompt", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Description of the website the user wants to build (e.g. 'ek modern cafe ki website banao')")
                    })
                })
                put("required", JSONArray().apply { put("prompt") })
            })
        })

        // 6. device_control
        functionDeclarations.put(JSONObject().apply {
            put("name", "device_control")
            put("description", "Control hardware toggles: flashlight/torch directly, or open settings panel for wifi or bluetooth.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("what", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'flashlight' (or 'torch'), 'wifi', or 'bluetooth'")
                    })
                    put("state", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'on' or 'off'")
                    })
                })
                put("required", JSONArray().apply {
                    put("what")
                    put("state")
                })
            })
        })

        // 7. media_control
        functionDeclarations.put(JSONObject().apply {
            put("name", "media_control")
            put("description", "Control media music playback: play, pause, next, previous.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'play', 'pause', 'toggle', 'next', 'previous'")
                    })
                })
                put("required", JSONArray().apply { put("action") })
            })
        })

        // 8. search_youtube
        functionDeclarations.put(JSONObject().apply {
            put("name", "search_youtube")
            put("description", "Search YouTube for a specific song, artist, video or channel.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search query for YouTube")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 9. search_spotify
        functionDeclarations.put(JSONObject().apply {
            put("name", "search_spotify")
            put("description", "Search Spotify music for a song, album, or artist.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Song or artist to search on Spotify")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 10. set_alarm
        functionDeclarations.put(JSONObject().apply {
            put("name", "set_alarm")
            put("description", "Set an alarm on the phone for a specific 24-hour hour and minute.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("hour", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Hour of the day (0-23)")
                    })
                    put("minute", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Minute (0-59)")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Alarm label")
                    })
                })
                put("required", JSONArray().apply {
                    put("hour")
                    put("minute")
                })
            })
        })

        // 11. set_timer
        functionDeclarations.put(JSONObject().apply {
            put("name", "set_timer")
            put("description", "Set a countdown timer on the phone in seconds.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("seconds", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Duration in seconds")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Timer label")
                    })
                })
                put("required", JSONArray().apply { put("seconds") })
            })
        })

        // 12. phone_status
        functionDeclarations.put(JSONObject().apply {
            put("name", "phone_status")
            put("description", "Check phone status including battery percentage, charging state, volume level, and Wi-Fi.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 13. set_volume
        functionDeclarations.put(JSONObject().apply {
            put("name", "set_volume")
            put("description", "Adjust the device audio or media volume level.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("level", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Volume percentage (0 to 100)")
                    })
                })
                put("required", JSONArray().apply { put("level") })
            })
        })

        // 14. get_current_datetime
        functionDeclarations.put(JSONObject().apply {
            put("name", "get_current_datetime")
            put("description", "Get the exact current real-time date, time, and day of the week.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 15. web_search
        functionDeclarations.put(JSONObject().apply {
            put("name", "web_search")
            put("description", "Search the web via browser for real-time information.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search query")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 16. navigate_to
        functionDeclarations.put(JSONObject().apply {
            put("name", "navigate_to")
            put("description", "Open Google Maps navigation directions to a location or address.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("destination", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Place, city, address or coordinates")
                    })
                })
                put("required", JSONArray().apply { put("destination") })
            })
        })

        // 17. open_camera
        functionDeclarations.put(JSONObject().apply {
            put("name", "open_camera")
            put("description", "Launch the phone's camera.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 18. start_sketch_drawing
        functionDeclarations.put(JSONObject().apply {
            put("name", "start_sketch_drawing")
            put("description", "Turn photos into animated pencil sketch drawings.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("source", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'pick' to select a new image or leave empty to use shared")
                    })
                })
            })
        })

        toolsArray.put(JSONObject().apply {
            put("functionDeclarations", functionDeclarations)
        })

        return toolsArray
    }
}
