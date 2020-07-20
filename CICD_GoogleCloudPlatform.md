# CI/CD con Google Cloud Platform (GCP)

En este documento se describen los pasos para realizar una implementación sencilla de
 [CI/CD en GCP](https://cloud.google.com/docs/ci-cd) usando:
* [Google Kubernetes Engine](https://console.cloud.google.com/kubernetes/) (GKE)
* [Google Cloud Build](https://console.cloud.google.com/cloud-build/)
* [Google Container Registry](https://console.cloud.google.com/gcr) (GCR)

IMPORTANTE:
**Antes de habilitar estos servicios**, es necesario saber que los mismos tienen un costo asociado
y que **se requiere habilitar la facturación para el proyecto**.
Se recomienda [leer sobre los precios de los mismos](https://cloud.google.com/pricing/list)
o estimar el costo mensual usando [la calculadora que Google provee](https://cloud.google.com/products/calculator)
para este fin.

## Continuous integration

La integración contínua va a automatizar el proceso de llevar el código fuente desde el repositorio git
hasta una imagen docker lista para su despliegue en Kubernetes.

En este proyecto la imagen docker se define (ver [Dockerfile](https://github.com/ezetarg/contact-validation/blob/master/Dockerfile))
de la siguiente manera:
```dockerfile
FROM openjdk:14-alpine
COPY build/libs/*-all.jar app.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "app.jar"]
```

### Configuración de Google Cloud Build

1. Habilitar el API de Google Cloud Build [(ver doc)](https://cloud.google.com/cloud-build/docs/quickstart-build#before-you-begin).

1. Crear el archivo [`cloudbuild.yaml`](https://github.com/ezetarg/contact-validation/blob/master/cloudbuild.yaml)
   en el proyecto para definir los pasos que Cloud Build debe ejecutar.
   - Compilación de la aplicación con gradle [(doc)](https://cloud.google.com/cloud-build/docs/building/build-java "ver más info sobre este builder").
     ```yaml
     steps:
       - name: gradle:jdk11
         entrypoint: gradle
         args:
           - 'build'
           - '-Pversion=$TAG_NAME'
     ```

   - Construcción de la imagen docker en base al Dockerfile definido
     [(doc)](https://cloud.google.com/cloud-build/docs/building/build-containers "ver más info sobre este builder").
     ```yaml
       - name: gcr.io/cloud-builders/docker
         args:
           - 'build'
           - '--tag=gcr.io/$PROJECT_ID/$REPO_NAME'
           - '--tag=gcr.io/$PROJECT_ID/$REPO_NAME:$TAG_NAME'
           - '.'
     ```

   - Publicación de la imagen docker en el Container Registry de Google
     [(ver doc para habilitar GCR)](https://cloud.google.com/container-registry/docs/pushing-and-pulling).
     ```yaml
       - name: gcr.io/cloud-builders/docker
         args:
           - 'push'
           - 'gcr.io/$PROJECT_ID/$REPO_NAME'
      ```

   - Como se puede ver en los puntos anteriores, existe la posibilidad de hacer uso de variables. Cloud Build provee algunas de forma automática
     [(ver doc)](https://cloud.google.com/cloud-build/docs/configuring-builds/substitute-variable-values)
     y otras se pueden enviar desde el trigger que ejecuta el build.
     Si deseamos darles un valor por defecto, se puede hacer en la siguiente sección: 
     ```yaml
     substitutions:
       TAG_NAME: $SHORT_SHA
     ```
     En este ejemplo se establece que si la variable TAG_NAME no está informada (si el build no se ejecuta sobre un tag de git),
     entonces debe tomar el valor de la variable SHORT_SHA.

   - También es posible establecer etiquetas a los builds para luego poder ubicarlos de manera más sencilla.
     ```yaml
     tags:
       - $REPO_NAME
       - demo
     ```

1. Conectar Cloud Build con el repositorio en Github [(ver doc)](https://cloud.google.com/cloud-build/docs/automating-builds/create-github-app-triggers).

1. Crear un trigger que haga referencia a la rama/tag de interés y al archivo yaml creado.

1. Para probar el trigger se puede añadir cambios a la rama/tag,
   o bien ejecutar manualmente el trigger desde el listado.

## Continuous Delivery / Continuous Deployment

### Configuración del cluster en GKE
Si no se cuenta aún con un cluster, se puede crear uno de forma sencilla con la opción que GCP ofrece como "my first cluster",
el cual proporciona por defecto las siguientes características:
* Cluster zone: **us-central1-c**
* Version: **Rapid release channel** (versión de Kubernetes, en este caso provee las actualizaciones más recientes)
* Machine type: **g1-small** (0.5 vCPU y 1.7 GB de RAM)
* Boot disk size: **32GB**
* Autoscaling: **Disabled**
* Cloud Operations for GKE: **Disabled** 

Esta configuración se puede personalizar. Por ejemplo, para comenzar a trabajar con GKE resulta suficiente contar con 1 cluster con 1 nodo en una sola zona.

Se puede encontrar más información en esta [documentación](https://cloud.google.com/kubernetes-engine/docs/how-to/creating-a-cluster).

_Nota: Para que Cloud Build pueda administrar el cluster, el API de Kubernetes debe ser accesible.
En caso de usar un cluster privado se sugiere revisar la [documentación](https://cloud.google.com/kubernetes-engine/docs/how-to/private-clusters)._


### Configuración del deployment automático con Google Cloud Build

1. Definir el deployment y el service. Se puede tomar como ejemplo esta definición:
   [complete.yaml](https://github.com/ezetarg/contact-validation/blob/master/k8s/complete.yaml)

1. Habilitar Kubernetes Engine en la [configuración de Cloud Build](https://console.cloud.google.com/cloud-build/settings/service-account).

1. Añadir a cloudbuild.yaml el paso necesario para el deploy
   ```yaml
     - name: gcr.io/cloud-builders/gke-deploy
       args:
         - 'run'
         - '--filename=k8s/complete.yaml'
         - '--image=gcr.io/$PROJECT_ID/$REPO_NAME:$TAG_NAME'
         - '--location=us-east1'
         - '--cluster=my-cluster-name'
   ```
   Se puede encontrar más información en la documentación del builder [deploy-gke](https://cloud.google.com/cloud-build/docs/deploying-builds/deploy-gke).

1. ¡Listo! El próximo despliegue se realizará de forma automática.

IMPORTANTE: Aunque este documento no se focaliza en el testing es importante mencionar que para automatizar el despliegue de la aplicación
 resulta necesario que exista también una automatización de las pruebas (unitarias, de integración, etc) para poder garantizar
 lo más posible que el software liberado sea de calidad.

#### Personalización de despliegue para distintos clusters
Para trabajar con distintos clusters, por ejemplo cuando se tiene un ambiente de staging, se puede emplear la misma definición
del archivo cloudbuild.yaml usando variables para indicar el cluster a utilizar en el despliegue de la siguiente manera:

1. Utilizar variables el paso del deploy.
   ```yaml
     - name: gcr.io/cloud-builders/gke-deploy
       args:
         - 'run'
         - '--filename=k8s/deployment.yaml'
         - '--image=gcr.io/$PROJECT_ID/$REPO_NAME:$TAG_NAME'
         - '--location=${_CLUSTER_ZONE}'
         - '--cluster=${_CLUSTER_NAME}'
   ```

1. Establecer valores por defecto para las variables.
   ```yaml
   substitutions:
     _CLUSTER_NAME: staging-cluster
     _CLUSTER_ZONE: us-central1-c
     TAG_NAME: $SHORT_SHA
    ```

1. Crear un trigger para cada cluster y especificar en cada uno el cluster a utilizar.

1. También es posible etiquetar de forma distinta las imágenes docker generadas.
   Por ejemplo:
   - Para el ambiente de staging puede dispararse el builder con cada push a master.
     En ese caso al no haber un `TAG_NAME`, se usará el `SHORT_SHA` del commit.
   - Para el ambiente de producción, el trigger puede dispararse al crearse un tag.
     En este caso se usaría el nombre del tag para etiquetar la imagen.

