package com.bulefire.neuracraft.compatibility.entity;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Content(@NotNull String type, @NotNull Object text) {
    @Contract("_, _ -> new")
    public static @NotNull Content of(@NotNull String type, @NotNull Object text) {
        return new Content(type, text);
    }
    
    @Contract(pure=true)
    public static @NotNull Content ifTypeOrThrow(@NotNull Content content, @NotNull String type){
         if (content.type.equals(type)) {
             return content;
         }
         throw new IllegalArgumentException("Content is not "+type);
    }
    
    @Contract(pure=true)
    public @NotNull String textOrThrow(){
        return (String) ifTypeOrThrow(this, Type.TEXT).text;
    }
    
    @Contract(pure=true)
    public @NotNull String imageData64OrThrow(){
        return (String) ifTypeOrThrow(this, Type.IMAGE_DATA_64).text;
    }
    
    @Contract(pure=true)
    public @NotNull String imageUrlOrThrow(){
        return (String) ifTypeOrThrow(this, Type.IMAGE_URL).text;
    }
    
    public static class Type {
        public static final String TEXT = "text";
        public static final String IMAGE_URL = "image_url";
        public static final String IMAGE_DATA_64 = "image_data_64";
    }
}