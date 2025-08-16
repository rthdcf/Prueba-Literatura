package com.alura.Literatura.service;

public interface IConvierteDatos {

    <T> T obtenerDatos(String Json, Class<T> clase);
}
