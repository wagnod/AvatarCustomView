# CustomAvatarImage
Практика в создании кастомных атрибутов.
 
<img src="https://github.com/wagnod/AvatarCustomView/blob/master/CustomImageView.gif" width="20%" />
 
### Что сделано?
   * Использование `Canvas API` для отрисовки стандартного прямоугольника, который будет хранить нашу картинку.
   * Дальше идут 2 случая отрисовки той картинки, которую мы видим в итоге. Обе вёрстки для тестов можнно найти в `activity_main.xml`.
      * В первом случае используем маски. Накладываем одну `Bitmap` на другую с помощью алгоритма `Porter-Duff`, который позволяет наложить `Bitmap`, содержащую закругление, на исходную картинку (`PorterDuff.Mode.SRC_IN`), тем самым получив обводку.
      * Во втором случае используем `Shader`. Он позволяет определить для объекта `Paint` содержимое, которое должно быть нарисовано. Используем `BitmapShader`, чтобы сказать, что для рисования нужно использовать `Bitmap`, которая хранит нашу исходную картинку.
   * `CustomAvatarImage` нарисован с использованием `Shader`, так как данный способ чуть более эффективный и выигрывает по производительности.
      * Для получения анимации был использован `ValueAnimator` + `LinearInterpolator` для равномерного увеличения ширины картинки.
      * Цвет фона меняется в зависимости от инициалов, указанных в `XML`.  
      * Полученный `CustomAvatarImage` сохраняет своё состояние при поворотах экрана.
