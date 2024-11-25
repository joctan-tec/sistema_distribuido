# Sisteman Distribuido
[Enlace al repositorio de github](https://github.com/joctan-tec/sistema_distribuido)
## Índice
1. [Documentación de Diseño](#documentación-de-diseño)
    - [Explicación de la arquitectura](#explicación-de-la-arquitectura) 
    - [Mecanismos de comunicación](#mecanismos-de-comunicación)
    - [Sincronización](#sincronización)
    - [Gestión de fallos](#gestión-de-fallos)
2. [Pruebas y resultados](#pruebas-y-resultados)
    - [Prueba 1: Asignación de Procesos y Balanceo de Carga](#1-asignación-de-procesos-y-balanceo-de-carga) 
    - [Prueba 2: Sincronización de Recursos Compartidos](#2-sincronización-de-recursos-compartidos)
    - [Prueba 3: Manejo de Fallos](#3-manejo-de-fallos)
    - [Prueba 4: Escalabilidad del Sistema](#4-escalabilidad-del-sistema)
    - [Prueba 5: Redistribución Automática de Procesos](#5-redistribución-automática-de-procesos)
## Documentación de Diseño
![Diagrama en blanco](https://github.com/user-attachments/assets/2f5a72a3-bdb5-4d45-b904-6df2445ce3ac)
### Explicación de la arquitectura

### Mecanismos de comunicación

### Sincronización

### Gestión de fallos

## Pruebas y resultados

### 1. Asignación de Procesos y Balanceo de Carga

**Objetivo:** Verificar que el sistema asigna los procesos al nodo adecuado y distribuye la carga entre los nodos.

**Entradas:** Carga inicial en cada nodo; proceso nuevo que debe ser asignado.

**Procedimiento:**

1. Asignar procesos a los nodos hasta alcanzar un nivel de carga variable en cada nodo.

2. Crear un nuevo proceso y solicitar su asignación.

4. Verificar que el proceso se asigna al nodo menos cargado.

**Resultados Esperados:** El proceso debe ser asignado al nodo que tenga menos carga en ese momento.

**Resultados Obtenidos:** 

En la siguiente imagen se puede ver que la interfaz a la izquierda confirma que se agregaron nodos y se ejeutaron las tareas. A la derecha en la terminal podemos ver en las letras amarillas que se le asignaron las tareas a cada nodo de manera satisfactoria. 
![image](https://github.com/user-attachments/assets/4f79449b-4d27-4326-b23b-74619c93eaf3)
### 2. Sincronización de Recursos Compartidos

**Actores:** Nodo
**Descripción:** Un nodo solicita acceso a un recurso compartido. Si el recurso está disponible, el nodo lo adquiere; si no, espera hasta que esté disponible para asegurar la exclusión mutua
**Precondición:** Existen recursos compartidos en la red, y los nodos pueden solicitar acceso a dichos recursos.
**Postcondición:** El recurso es asignado al nodo solicitante o el nodo entra en estado de espera hasta que el recurso esté disponible.
**Flujo Normal:**
1. Un nodo solicita acceso a un recurso compartido.
2. El sistema verifica la disponibilidad del recurso solicitado.
3. Si el recurso está disponible, el nodo adquiere el recurso y lo utiliza.
4. Al finalizar, el nodo libera el recurso para que otros nodos puedan acceder a él.

En los logs podemos ver que se escriben tres carnets en tiempos diferentes, de manera secuencial. Solo un nodo tenía acceso al recurso (que sería el archivo de texto) y este no estaba disponible para otros nodos haste que se terminara de ejecutar el proceso. Los carnets se generan en las líneas 6, 7 y 8. 

![image](https://github.com/user-attachments/assets/006941fe-50a6-48b7-aa45-a9b52f0a415a)

### 3. Manejo de Fallos

**Objetivo:** Verificar que el sistema redistribuye correctamente los procesos en caso de fallo de un nodo.

**Entradas:**  Nodo en ejecución, procesos asignados al nodo, fallo simulado.

**Procedimiento:**

1. Asignar varios procesos a un nodo específico.

2. Simular el fallo de dicho nodo.

3. Observar la redistribución de los procesos en los nodos restantes.

**Resultados Esperados:** Los procesos deben redistribuirse y ejecutarse en los nodos restantes sin interrumpir el sistema

**Resultados Obtenidos:** 

Aquí podemos ver en la izquierda que los carnets fueron generados correctamente. A la derecha en la terminal vemos en letras blancas que el nodo fue marcado como muerto, este es el fallo realizado al propio al eliminar el nodo. Luego de esto podemos ver que se redistribuyeron las tareas. 

![image](https://github.com/user-attachments/assets/244a6481-3008-4661-8e81-ff67f11985cc)

Para facilitar la lectura, agregamos una imagen de la terminal como tal:

![image](https://github.com/user-attachments/assets/86fd0c67-a3c2-45ef-91dd-20e80e0b6275)

### 4. Escalabilidad del Sistema
**Objetivo:** Evaluar la capacidad del sistema para agregar nuevos nodos sin afectar su funcionamiento.

**Entradas:** Estado inicial de la red distribuida, nuevos nodos a agregar.

**Procedimiento:**

1. Ejecutar el sistema con un conjunto de nodos iniciales.

Podemos ver que se agregaron dos nodos iniciales en la terminal a la derecha. En letra verde los healthchecks realizados periódicamente validan que no hay interrupciones.  
![image](https://github.com/user-attachments/assets/eb791a96-4274-4b54-8b17-da25391badca)

2. Agregar nuevos nodos a la red distribuida mientras los procesos están en ejecución.

Agregamos dos nodos nuevos, a la izquierda vemos en la interfaz que se agregaron exitosamente.    
![image](https://github.com/user-attachments/assets/72117a4d-b0b3-4f3c-87d6-8457535df83b)

3. Verificar que el sistema integre los nuevos nodos sin interrupciones.

Confirmamos que los dos nodos fueron agregados sin interrumpir que los dos primeros nodos ejecuten sus tareas y las completen. 
![image](https://github.com/user-attachments/assets/1e1d0647-f48d-4cfc-b83b-f8d7c76e85a2)

**Resultados Esperados:** El sistema debe aceptar los nuevos nodos y redistribuir la carga de procesos sin interrupciones.

**Resultados Obtenidos:**

Para validar, podemos observar en la terminal que los healthchecks confirmaron que los nuevos nodos fueron agregados al sistema y los nodos anteriores ejecutaron sus tareas. 

![image](https://github.com/user-attachments/assets/7ff80d99-87d3-493f-be0a-4c9a335fda86)

### 5. Redistribución Automática de Procesos

**Objetivo:** Comprobar que el sistema redistribuye los procesos de manera automática si un nodo alcanza su máxima capacidad de carga.

**Entradas:** Número de procesos y límite de capacidad de carga en los nodos.

**Procedimiento:**

1. Asignar múltiples procesos a los nodos hasta que uno de los nodos alcance su límite de carga.

2. Observar si el sistema redistribuye los procesos excedentes a otros nodos disponibles.

**Resultados Esperados:** l sistema debe redistribuir automáticamente los procesos al nodo más adecuado.

**Resultados Obtenidos:** 

A la derecha en la terminal vemos en letras blancas que el nodo fue marcado ya que excedió su límite de 4 tareas. Luego de esto podemos ver que se redistribuyeron las tareas. 

![image](https://github.com/user-attachments/assets/116e64ee-f82a-4ade-80ad-088552fdb811)

Para facilitar la lectura, agregamos una imagen de la terminal como tal:

![image](https://github.com/user-attachments/assets/090db5bf-c459-4dc4-b413-93addce6ab6c)

