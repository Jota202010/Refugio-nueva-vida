# Datos de demostración de Docker

Estas cuentas son únicamente para la demostración local. No uses estas contraseñas en un despliegue público. La aplicación almacena las contraseñas con hash; este archivo permite iniciar sesión en la demostración.

| Usuario | Contraseña | Rol |
| --- | --- | --- |
| `admin` | `demo12345` | Administrador |
| `adoptante1` | `Adopta2026!` | Usuario |
| `adoptante2` | `Luna2026!` | Usuario |
| `adoptante3` | `Max2026!` | Usuario |

Las cuentas y los perros de ejemplo se crean si no existen. Los datos que ya estén en la base no se reemplazan. Las fotos de ejemplo se copian al volumen persistente de imágenes de Docker.

## Créditos de las imágenes

Las imágenes se descargaron de Wikimedia Commons en tamaño reducido. Los nombres de Luna, Max, Coco, Rocky, Nala y Bruno son personajes ficticios del conjunto de demostración; las fotos son ilustrativas y no representan perros reales disponibles para adopción.

| Perro | Imagen original | Autor | Licencia |
| --- | --- | --- | --- |
| Luna | [Dog portrait Budapest](https://commons.wikimedia.org/wiki/File:Dog_portrait_Budapest.jpg) | CONTRERAS Roberto | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| Max | [Close-up portrait of dog](https://commons.wikimedia.org/wiki/File:Close-up_portrait_of_dog.jpg) | Pittigrilli | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| Coco | [Chinese crested dog on the beach](https://commons.wikimedia.org/wiki/File:Chinese_crested_dog_on_the_beach_(92633).jpg) | Rhododendrites | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| Rocky | [Portrait of a black and white mixed breed dog outdoors](https://www.pexels.com/photo/portrait-of-a-black-and-white-mixed-breed-dog-outdoors-28405671/) | Gundula Vogel | [Licencia Pexels](https://www.pexels.com/license/) |
| Nala | [Golden Retriever in sunlit field portrait](https://www.pexels.com/photo/golden-retriever-in-sunlit-field-portrait-34777037/) | Lilly Grace | [Licencia Pexels](https://www.pexels.com/license/) |
| Bruno | [Sledge dog portrait](https://commons.wikimedia.org/wiki/File:Sledge_dog_portrait.jpg) | Frank Hurley | Dominio público |

Las licencias CC BY y CC BY-SA permiten reutilización bajo sus condiciones de atribución; CC BY-SA requiere compartir las adaptaciones bajo la misma licencia. Los archivos se conservan sin edición en `src/main/resources/demo/dogs/`.
