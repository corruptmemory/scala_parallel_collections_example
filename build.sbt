// set the name of the project
name := "Parallel Collection Example"

version := "1.0"

organization := "com.corruptmemory"

scalaVersion := "2.9.0-1"

// define the statements initially evaluated when entering 'console', 'console-quick', or 'console-project'
initialCommands := """
  import System.{currentTimeMillis => now}
  def time[T](f: => T): T = {
    val start = now
    try { f } finally { println("Elapsed: " + (now - start)/1000.0 + " s") }
  }
  import com.corruptmemory.par_example._
  import com.corruptmemory.par_example.ParExample._
"""
