/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg
package android

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.EntryPointFinder
import org.opalj.br.{ClassFile, Method, MethodDescriptor, ObjectType, ReferenceType}
import org.opalj.tac.fpcf.analyses.MethodDescription

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.CollectionHasAsScala


/**
 * The AndroidEntryPointFinder considers specific methods of launcher Activity Clases as entry points.
 * An activity is a launcher activity if it contains an intent filter with action "android.intent.action.MAIN"
 * and category "android.intent.category.LAUNCHER". Requires Android Manifest to be loaded.
 *
 * @author Tom Nikisch
 *         Julius Naeumann
 */
object AndroidEntryPointsFinder extends EntryPointFinder {
  val configKey = "org.opalj.tac.cg.android.AndroidEntryPointsFinder.entryPoints"
  override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
    val entryPointDescriptions = getConfiguredEntryPoints(project)
    val manifest: AndroidManifest = project.get(AndroidManifestKey).get
    // get launcher
    val launchableActivities = manifest.components.collect { case a : Activity => a }.filter(_.isLauncherActivity)
    val launchableClasses = launchableActivities.map(_.cls)
    val classHierarchy = project.classHierarchy
    val entryPoints = ArrayBuffer[Method]()

    // collect entry point methods from launchable activities
    for (componentClass <- launchableClasses) {
      for (epd <- entryPointDescriptions) {
        if (classHierarchy.isASubtypeOf(ReferenceType(componentClass.fqn), ReferenceType(epd.cf)).isYes) {
          entryPoints ++= componentClass.findMethod(epd.name, MethodDescriptor(epd.desc))
        }
      }
    }
    entryPoints
  }

  private def getConfiguredEntryPoints(project: SomeProject) = {
    val entryPointDescriptionsConfig = project.config.getConfigList(configKey).asScala.toArray
    val entryPointDescriptions = entryPointDescriptionsConfig.map(c => MethodDescription.reader.read(c, ""))
    entryPointDescriptions
  }
}
