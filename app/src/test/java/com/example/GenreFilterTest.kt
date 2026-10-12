package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreFilterTest {

  data class MovieContent(
    val id: String,
    val firebaseId: String,
    val title: String,
    val type: String = "anime", // anime, animation
    val category: String = "مسلسل", // مسلسل, فلم, أوفا, خاصة
    val version: String = "مترجم", // مترجم, مدبلج
    val year: Int = 2024,
    val genres: List<String> = emptyList(),
    val description: String = ""
  )

  data class FilterState(
    val kind: String = "all",
    val version: String = "all",
    val year: String = "all",
    val genres: List<String> = emptyList()
  )

  companion object {
    fun matchesFilters(item: MovieContent, targetSection: String, filter: FilterState): Boolean {
      // 1. Target section
      if (targetSection == "anime") {
        if (item.type != "anime" && item.type != "foreign" && item.type != "news" && item.type != "entertainment") return false
      } else if (targetSection == "animation") {
        if (item.type != "animation" && item.type != "arabic" && item.type != "sports") return false
      }

      // 2. Kind of work (مسلسل / فلم)
      if (filter.kind != "all") {
        val k = filter.kind.trim()
        val c = item.category.trim()
        if (k == "فلم" || k == "فيلم") {
          if (c != "فلم" && c != "فيلم") return false
        } else {
          if (c != k) return false
        }
      }

      // 3. Version (مترجم / مدبلج)
      if (filter.version != "all") {
        val v = filter.version.trim()
        if (!item.version.contains(v) && !item.title.contains(v) && !item.description.contains(v)) {
          return false
        }
      }

      // 4. Year
      if (filter.year != "all") {
        if (item.year.toString() != filter.year) return false
      }

      // 5. Genres: Must contain ALL selected genres
      if (filter.genres.isNotEmpty()) {
        for (g in filter.genres) {
          val has = item.genres.contains(g) || item.title.contains(g) || item.description.contains(g) || item.category.contains(g)
          if (!has) return false
        }
      }

      return true
    }
  }

  @Test
  fun testSingleGenreFiltering() {
    val slamDunk = MovieContent(
      id = "1",
      firebaseId = "fb-1",
      title = "سلام دانك",
      genres = listOf("رياضي", "كوميدي", "مدرسي"),
      category = "مسلسل",
      version = "مدبلج",
      year = 1993
    )

    val attackOnTitan = MovieContent(
      id = "2",
      firebaseId = "fb-2",
      title = "هجوم العمالقة",
      genres = listOf("أكشن", "غموض", "دراما", "عسكري"),
      category = "مسلسل",
      version = "مترجم",
      year = 2013
    )

    val list = listOf(slamDunk, attackOnTitan)

    // Filter by genre "رياضي"
    val sportsFilter = FilterState(genres = listOf("رياضي"))
    val sportsResults = list.filter { matchesFilters(it, "anime", sportsFilter) }
    assertEquals(1, sportsResults.size)
    assertEquals("سلام دانك", sportsResults[0].title)

    // Filter by genre "أكشن"
    val actionFilter = FilterState(genres = listOf("أكشن"))
    val actionResults = list.filter { matchesFilters(it, "anime", actionFilter) }
    assertEquals(1, actionResults.size)
    assertEquals("هجوم العمالقة", actionResults[0].title)
  }

  @Test
  fun testMultipleGenresFiltering() {
    val haikyuu = MovieContent(
      id = "1",
      firebaseId = "fb-1",
      title = "هايكيو!!",
      genres = listOf("رياضي", "مدرسي", "كوميدي", "شباب"),
      category = "مسلسل",
      version = "مترجم",
      year = 2014
    )

    val kuroko = MovieContent(
      id = "2",
      firebaseId = "fb-2",
      title = "كوروكو نو باسكت",
      genres = listOf("رياضي", "مدرسي", "قوة خارقة"),
      category = "مسلسل",
      version = "مترجم",
      year = 2012
    )

    val list = listOf(haikyuu, kuroko)

    // Works that have both "رياضي" and "كوميدي"
    val filterBoth = FilterState(genres = listOf("رياضي", "كوميدي"))
    val bothResults = list.filter { matchesFilters(it, "anime", filterBoth) }
    assertEquals(1, bothResults.size)
    assertEquals("هايكيو!!", bothResults[0].title)

    // Works that have both "رياضي" and "مدرسي" -> both match
    val filterCommon = FilterState(genres = listOf("رياضي", "مدرسي"))
    val commonResults = list.filter { matchesFilters(it, "anime", filterCommon) }
    assertEquals(2, commonResults.size)

    // Non-existent genre combination
    val filterNone = FilterState(genres = listOf("رياضي", "رعب"))
    val noneResults = list.filter { matchesFilters(it, "anime", filterNone) }
    assertEquals(0, noneResults.size)
  }

  @Test
  fun testCombinedFiltersYearKindVersionGenres() {
    val yourName = MovieContent(
      id = "1",
      firebaseId = "fb-1",
      title = "اسمك (Your Name)",
      category = "فلم",
      version = "مترجم",
      year = 2016,
      genres = listOf("رومانسي", "خيال", "دراما", "خارق للطبيعة")
    )

    val silentVoice = MovieContent(
      id = "2",
      firebaseId = "fb-2",
      title = "صوت صامت",
      category = "فلم",
      version = "مدبلج",
      year = 2016,
      genres = listOf("دراما", "مدرسي", "شريحة من الحياة")
    )

    val list = listOf(yourName, silentVoice)

    // 2016 + فلم + مترجم + رومانسي
    val f1 = FilterState(kind = "فلم", version = "مترجم", year = "2016", genres = listOf("رومانسي"))
    val res1 = list.filter { matchesFilters(it, "anime", f1) }
    assertEquals(1, res1.size)
    assertEquals("اسمك (Your Name)", res1[0].title)

    // 2016 + فلم + مدبلج
    val f2 = FilterState(kind = "فلم", version = "مدبلج", year = "2016")
    val res2 = list.filter { matchesFilters(it, "anime", f2) }
    assertEquals(1, res2.size)
    assertEquals("صوت صامت", res2[0].title)
  }
}
