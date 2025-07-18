# EC2 duplicados con AMI y Load Balancer
Este proyecto consiste en la creación de una instancia EC2 que montara un servidor HTTP básico que luego será replicada creando imágenes a través del servicio AMI y posteriormente estas múltiples instancias serán gestionadas a través de un load balancer.

## 1. Creación de la instancia EC2
Para la creación de la instancia EC2 se utilizarán los recursos básicos proporcionados por la "capa gratuita"
- Se utiliza imagen Amazon Linux
- Tipo de instacia t2.micro
- Se implementará un script inicial para que instale las dependencias necesarias para luego replicarlo a través de AMI
- Se elige un grupo de seguridad personalizado (se explicará más adelante)

![Creación del EC2](images/creacion-ec2.png)

![Bienvenida instancia EC2 HTTP](images/ec2-servidor-bienvenida.png)

### 1.1 Creación de grupo de seguridad
Los grupos de seguridad con firewalls para controlar las "conexiones" de entrada y de salida. En este caso a modo de práctica se define uno personalizado permitiendo cualquier salida y restringiendo la entrada a conexiones SSH y HTTP

![Creación grupo de seguridad](images/grupo-seguridad.png)

### 1.2 Configuración de la instancia para crear imagen
Para configurar la máquina y luego replicarla a través de imágenes se ejecuta el siguiente script:
~~~bash
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
echo "<h1>Hola desde el EC2: $(hostname -f)</h1>" > /var/www/html/index.html
~~~

Esto script simplemente actualiza los paquetes e instala un servidor, que se iniciara siempre que la instancia este encendida. Luego se crea un index con la identificación de esta instancia. Para simplificar el acceso a la instancia se llevo a cabo a través de la consola, sin embargo podría realizarse por medio de una conexión SSH "manual" configurando claves para la instancia.

![Ejecutando script en la instancia](images/set-up-script.png)

## 2. Creación de imagen con AMI
Se lleva a cabo la creación de una imagen a partir de la instancia creada anteriormente

![Creación imagen AMI](images/creacion-imagen.png)

![Catalogo personal AMI](images/catalogo-personal-ami.png)

## 3. Replicación de instancia con AMI
Teniendo la imagen creada, se crean 2 instancias adicionales. En cada nueva instancia se ejecuta el comando:
~~~bash
echo "<h1>Hola desde el EC2: $(hostname -f)</h1>" > /var/www/html/index.html
~~~
para poder diferenciar cada instancia al conectarse posteriormente a través del balanceador de carga

![Lista de instancias EC2](images/catalogo-ec2.png)

Nota: En todas las replicaciones se utilizó el grupo de seguridad creado anteriormente

## 4. Creación del Load Balancer
Entre las configuraciones más importantes se destacan:
- El balanceador es de tipo "aplicación" ya que se realiza el balance a nivel de redirección de las peticiones HTTP
- Se utilizó el grupo de seguridad creado anteriormente
- Se creo un grupo de destino con las diferentes instancias EC2

![Creación balanceador](images/creacion-balanceador.png)

### 4.1 Creación grupo destino
Simplemente se configuro el grupo añadiendo todas las instancias y redirigiendo las peticiones hacia el puerto 80 (HTTP)

![Creación grupo destino](images/creacion-grupo-destino.png)

Finalmente se muestra un video con el funcionamiento del balanceador de carga

![Gif probando el balanceador](images/prueba-balanceador.gif)

