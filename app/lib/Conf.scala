package lib

import play.api.Play

trait Conf {
   implicit def conf = Play.maybeApplication.get.configuration
 }
