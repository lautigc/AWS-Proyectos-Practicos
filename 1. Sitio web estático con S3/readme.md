# Hospedar un sitio web estático (S3)
El objetivo de este proyecto es el hosting de un sitio web estático a través de la consola principal utilizando principalmente el servicio de S3.

## 1. Creación del bucket
Se crea un bucket con la configuración básica o por defecto. Se destacan las siguientes configuraciones:
- Bloquear todo el acceso público
  - Esta configuración bloqueará el acceso a todo público. Más allá de que hayan políticas establecidas que permitan el acceso, es un mecanismo para bloquear cualquier configuración existente y proteger el bucket de forma simple.
- Control de versiones de buckets
  - Esta opción es útil para poder versionar los cambios en el bucket (similar a git/github). En este caso dado que no tiene relevancia se deshabilita. Pero en un caso de uso real es altamente útil y necesario

![Creación bucket](images/S3-web/bucket-creacion.png)

## 2. Configuración como sitio web estático
Una vez creado el bucket S3 se configura el "alojamiento de sitios web estáticos". Se mantienen las configuraciones básicas, se destacan las siguientes configuraciones
- Documentos de error e índice
  - Se mantienen los nombres default "index.html" y "error.html"

![Configuración alojamiento web estático](images/S3-web/alojamiento-estático.png)

### 3. Se agregan los archivos correspondientes
Se agregan los archivos html básicos para el sitio web estático

![Carga archivos HTML](images/S3-web/carga-html.png)

### 4. Configuración acceso público
Como se destacó en el paso (1) se bloqueó el acceso a todo publíco (De igual manera, por más que este deshabilitada la opción no existía tampoco una política de acceso establecida):

![Acceso denegado](images/S3-web/acceso-denegado.png)

De esta manera, se deshabilita la opción y se configura una política de acceso abierta a todo público.
- Se utiliza el generador de políticas, por simplicidad.

![Generador de política](images/S3-web/generador-politica.png)

La política generada fue la siguiente:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Statement1",
      "Effect": "Allow",
      "Principal": "*",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::lgc-s3-estatico/*"
    }
  ]
}
```
- Efecto: Refiere al tipo de permiso, los que hay son allow/deny
- Principal: Refiere a quien aplica el permiso, en este caso a todos los usuarios
- Action: Representa el permiso en si mismo. En este caso getObject permite acceder a los "objetos" (asi se denominan a los archivos en este contexto de S3)
- Resource: Indica el recurso al cual aplica. Se especifica a través del ARN (Amazon Resource Name). Notar que luego del ARN se agrega el símbolo "*" (comodín o all) que refiere a todos los objetos del bucket.

![Política del bucket configuración](images/S3-web/politica-bucket.png)

Así finalmente puede accederse al bucket:

![Página de inicio](images/S3-web/welcome-index.png)

Y si se intenta algo inválido (Ej: url inválida):

![Página de error](images/S3-web/error-html.png)

## 5. Extensión con CloudFront

### 5.1 Creación de distribución cloud front
Se crea la distribución cloud frount. Se mantiene la configuración básica, entre las configuraciones más relevantes se destaca:
- Origen
  - Se selecciona el bucket S3 creado anteriormente

- Seguridad
  - Se desactiva WAF, este firewall es útil para proteger adecuadamente el sistema, sin embargo dado que este caso es una simple prueba se desactiva (además tiene costos adicionales)

Finalmente el servicio de cloud front fue creado y pudo accederse a la web s3 estática correctamente (nótese el cambio de nombre de dominio porque ahora se encuentra por encima la capa de CloudFront por sobre el S3):

![Resumen CF creado](images/S3-web/cf-s3-estatico.png)

![Sitio web](images/S3-web/cf-s3-sitio-web.png)

## 6. Extensión. Despliegue con CDK Java
CDK es una herramamienta que permite definir la infraestructura como código (IaC). De esta manera, para poder replicar el trabajo realizado fácilmente, se implementa el código correspondiente. El código que se explicará forma parte de la clase "Stack" que representa la infraestructura a desplegar, el código en su contexto completo puede verse en [Ir a la carpeta s3-web-estatico-despliegue](../CDK-CloudFormation-Deploys/s3-web-estatico-despliegue/).

~~~Java
BlockPublicAccess noblock = BlockPublicAccess.Builder.create()
    .blockPublicPolicy(false)
    .ignorePublicAcls(false)
    .blockPublicAcls(false)
    .restrictPublicBuckets(false)
    .build();

// Se crea el bucket
Bucket bucket = Bucket.Builder.create(this, "s3-cdk-web-estatico")
    .websiteIndexDocument("index.html")
    .websiteErrorDocument("error.html")
    .removalPolicy(RemovalPolicy.DESTROY)
    .autoDeleteObjects(true)
    .blockPublicAccess(noblock)
    .build();
~~~
- La variable noblock representa la desactivación del bloqueo de acceso público. Así posteriormente se definirá una política que permitirá el acceso a los objetos del bucket
- La creación del bucket responde a la configuración que se definió con la consola
  - Se configuran como archivos de inicio y error a "index.html" y "error.html"
  - Algo importante a tener en cuenta es que cuando esta infraestructura se "elimine o destruya", para que el bucket también se elimine se activa la política "DESTROY" y además la eliminación de los objetos contenidos.

~~~Java
// Agregar archivos html
BucketDeployment.Builder.create(this, "HTMLFiles")
        .sources(List.of(Source.asset("html-files/")))
        .destinationBucket(bucket)
        .build();

// Se agrega la política para permitir el acceso público
bucket.addToResourcePolicy(PolicyStatement.Builder.create()
      .effect(Effect.ALLOW)
      .principals(List.of(new AnyPrincipal()))
      .actions(List.of("s3:GetObject"))
      .resources(List.of(bucket.getBucketArn() + "/*"))
      .build()
);
~~~
- A través del objeto o elemento "BucketDeployment" se realiza la carga de los archivos al bucket
  - En este caso los archivos se almacenan en la carpeta "html-files" y se pone como destino el bucket definido anteriormente

 ~~~Java
// Se agrega la política para permitir el acceso público
bucket.addToResourcePolicy(PolicyStatement.Builder.create()
      .effect(Effect.ALLOW)
      .principals(List.of(new AnyPrincipal()))
      .actions(List.of("s3:GetObject"))
      .resources(List.of(bucket.getBucketArn() + "/*"))
      .build()
);
~~~
- Se agrega la política que permite el acceso a los recursos
  - La correspondencia es bastante directa. Algo quizá extraño es el uso de la clase "AnyPrincipal" que simplemente representa el comodín (*) o "cualquier usuario"
~~~Java
// Creación del CloudFront
Distribution.Builder.create(this, "cf-s3-web-estatico")
    .defaultBehavior(
        BehaviorOptions.builder()
            .origin(
                new S3StaticWebsiteOrigin(
                    bucket,
                    S3StaticWebsiteOriginProps.builder()
                        .build()
                )
            )
            .build()
    )
    .build()
    .applyRemovalPolicy(RemovalPolicy.DESTROY);
~~~
- Se define el CloudFront
  - Se especifica el origen, un sitio web estático S3
  - Se agrega nuevamente la política de "DESTROY" para eliminar el recurso al destruirse la infraestructura
