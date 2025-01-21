package com.aluracursos.LiterAlura.repository;

public interface IConvierteDatos {
    <T> T obtenerDatos(String json, Class<T> clase);
}