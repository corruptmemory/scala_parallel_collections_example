package com.corruptmemory.par_example

// Trivial example of Scala Parallel Collections
object ParExample {
  import scala.collection.mutable.ArraySeq
  import java.util.regex.Pattern
  import System.{currentTimeMillis => now}

  // Load Alice in Wonderland
  def load:Option[String] = {
    import java.io.{File,FileInputStream,IOException,FileNotFoundException}
    import java.lang.SecurityException
    import scala.util.control.Exception._
    val fi = new File("./wonder.txt")
    val buffer = new Array[Byte](fi.length.toInt)
    allCatch.either(new FileInputStream(fi)) match {
      case Left(_:FileNotFoundException) => {
        println("%s not found".format(fi))
        None
      }
      case Left(_:SecurityException) => {
        println("Cannot access file:".format(fi))
        None
      }
      case Right(fis) => allCatch.either(fis.read(buffer)) match {
        case Left(x:IOException) => {
          println(x.toString)
          allCatch.opt(fis.close)
          None
        }
        case Right(_) => {
          fis.close
          Some(new String(buffer))
        }
      }
    }
  }

  // Look for consecutive vowels
  final val pairVowles = Pattern.compile(".*[aeiouy]{2}",Pattern.CASE_INSENSITIVE)
  final val whitespace = """\s+""".r

  // You know, split things.
  def doSplit(in:String):Array[String] = whitespace.split(in)

  def matches(in:String):Boolean =
    pairVowles.matcher(in).matches

  // Parallel find words with 2 consecutive vowels
  def parFindPairVowels(in:Array[String]):ArraySeq[String] =
    (for (w <- in.par if matches(w)) yield w).seq

  // Parallel count words with 2 consecutive vowels
  def parCountPairVowels(in:Array[String]):Int =
    in.par.count(w => matches(w))

  // Sequential find words with 2 consecutive vowels
  def seqFindPairVowels(in:Array[String]):ArraySeq[String] =
    for (w <- in if matches(w)) yield w

  // Sequential count words with 2 consecutive vowels
  def seqCountPairVowels(in:Array[String]):Int =
    in.count(w => matches(w))

  // Helper routine to tweak Fork/Join
  // On my laptop 10 seems to give good performance
  def setParLevel(parlevel:Int) {
    collection.parallel.ForkJoinTasks.defaultForkJoinPool.setParallelism(parlevel)
  }

  // Helper routine for displaying function timing
  def time[T](f: => T): T = {
    val start = now
    try { f } finally { println("Elapsed: " + (now - start)/1000.0 + " s") }
  }

  // Generic benchmark helper
  def benchmark(in:Array[String])(func:Array[String] => Unit) {
    var i = 1000
    while (i>0) {
      func(in)
      i -= 1
    }
  }

  // Benchmark parallel find
  def benchmarkParFind(in:Array[String]) {
    benchmark(in)(parFindPairVowels _)
  }

  // Benchmark parallel count
  def benchmarkParCount(in:Array[String]) {
    benchmark(in)(parCountPairVowels _)
  }

  
  // Benchmark sequential find
  def benchmarkSeqFind(in:Array[String]) {
    benchmark(in)(seqFindPairVowels _)
  }

  // Benchmark sequential count
  def benchmarkSeqCount(in:Array[String]) {
    benchmark(in)(seqCountPairVowels _)
  }

  def repeat(count:Int)(body:Int => Unit) {
    var i = 1
    while(i <= count) {
      body(i)
      i += 1
    }
  }

  def main(args:Array[String]) {
    load.foreach {
      doc => {
        val words = doSplit(doc)
        val repeatCount = 10
        setParLevel(10) // Play with this 
        println("Warmup sequential")
        repeat(repeatCount) {
          run => {
            println("%d: sequential warmup".format(run))
            benchmarkSeqCount(words)
          }
        }
        println("\n\n**RUN sequential")
        repeat(repeatCount) {
          run => {
            println("%d: sequential RUN".format(run))
            time(benchmarkSeqCount(words))
          }
        }
        println("\n\nWarmup parallel")
        repeat(repeatCount) {
          run => {
            println("%d: parallel warmup".format(run))
            benchmarkParCount(words)
          }
        }
        println("\n\n**RUN parallel")
        repeat(repeatCount) {
          run => {
            println("%d: parallel RUN".format(run))
            time(benchmarkParCount(words))
          }
        }
      }
    }
  }

}
