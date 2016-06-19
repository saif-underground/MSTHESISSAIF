                     PREPARING UTEP THESES WITH LATEX
                             Luc Longpré

                             Mar-05-2008

===============================================================================
Modified since from the 2005 version:
- The name of Dean of Graduate school can be assigned in the main file
- Optional copyright page
- Can put a degree after the author's name on the title page
- The statement "This thesis was typed by the author" is no longer
  required in the vitae page
- Increased margins by .05 inches (gives a .05in room for measurement error)
- If you have figures or tables, look at examples in chapter 7 ( this is
  unchanged from 2005) 
===============================================================================

                             Apr-14-2005

===============================================================================
WHY USE LATEX?

LaTeX is a Document Preparation System, designed to produce a 
high-quality typesetting, especially for scientific text.

Many of our students initially plan to use Microsoft Word to
typeset their thesis.  Basic operations of Microsoft Word are
simple to learn, and what you see on the screen is what (usually)
gets printed.  You can easily include graphics and modify the
format to suit your purpose.

However, there are several drawbacks in using Word. It takes
a lot of work to make formulae look right, and the work needs
to be repeated for each formula.  In Word, many things are
done automatically, and when these things are not what you
want, it is often not clear how to fix the problem. Also,
sometimes Word will change things in your file and often
you don't notice what has happened.

LaTeX, in contrast, does typesetting. So, you edit your
source file, and then LaTeX typesets the source file. You can
then print the typeset document or look at it with a document
viewer. 

There are several advantages in using LaTeX for typesetting
your thesis.  First of all, it is quite easy to include
formulae in a document.  More importantly, the source file
contains logical design, not typeset material.  For example,
quotes are typed between a \begin{quote} and a \end{quote}.
This means all quotes will be formatted the same way,
the way the quote environment has been defined.
Formatting for these environments has been designed
by trained typographists.  Using these predefined environments
keep the authors from producing an aesthetically pleasing but
poorly designed document. But if you somewhat need to modify
how quotes are displayed, you can modify how quote environments
are displayed. Here again, just one modification changes the
appearance of all quotes. You don't have to search and modify
every occurrence in your document. In any case, when you write
your documents, you should be concerned with content and logical
structure, not with appearance. For more on this topic, look at
page 7 of Lamport's LaTeX book (see below for reference).

Lastly, most conferences require .pdf files for the submissions
and provide formatting files for submitted papers. Often it is
difficult to have the .pdf file generated from a Word document
meet the formatting requirement of the conference.

===============================================================================
BIBTEX

BibTeX is a separate program that extracts references from
a database and includes it in the LaTeX output document.
You create a database that specifies author, title, year,
journal and so on, for each citation, using a normal text
editor. Context aware editors like WinEdt and WinEmacs makes
this process easier.  When the text is processed, LaTeX collects
the citations, BibTeX extracts from the appropriate data from
the database and automatically generates the bibliography.


===============================================================================
INSTALLATION OF LaTeX on WINDOWS PC
           
Many of our students enjoy using WinEdt, which you can find
at www.winedt.com.
WinEdt is a powerful editor with a strong predisposition
towards the creation of LaTeX documents. A student license is $30
and it's $40 for educational purpose. There is a free trial period.

As for me, I more or less followed the instructions from
//http://www.math.aau.dk/~dethlef/Tips/introduction.html
to install WinEmacs and MiKTeX.
Although I skipped the `prepare' instructions,
it seems to work fine. Below are some details.

For LaTeX, I use MiKTeX, which is a TeX implementation
for the Windows operating system. I installed version 2.4.
The Web site address is http://www.miktex.org

Originally, I installed WinEmacs version 21.3, a GNU Project software.
Now, computers in the department have a newer version.
You can find a full binary version at the address:
http://ftp.gnu.org/pub/gnu/emacs/windows/
First look at the README file in that directory.

I also had to install AUCTeX, a package that extends emacs with
several macros for when emacs is used for LaTeX.

===============================================================================
HISTORY

Karen Villaverde wrote her computer science master's thesis in 1993
using LaTeX. She gracefully allowed us to use her thesis as an example.
In 1996, Yulia Kahl and Patrick Kahl both used her thesis as a base for
their own respective theses, which they in turn provided as examples. Their
files were used as templates by many of our students. 

Soon after this, LaTeX 2e came out. Transition to LaTeX 2e was slow, perhaps
because of backward compatibility. Some of our students have undoubtedly
upgraded and made various improvements over the years, passing the files
to each other. I prepared this version so our students don't have to
worry too much about the format of their thesis, and spend more time
on creative contents. The thesis in these files is Patrick Kahl's thesis,
modified to meet the current Graduate School requirements and to make
it simpler to use by our students. I added an extra 7th chapter to illustrate
tables, figures, and the inclusion of a .pdf file. I also created a BibTeX
file for the references, in case you would like to use this tool. But
mainly, I have moved as many formatting command into the package and
created a number of macros, to hide most of the gritty details.


===============================================================================

GENERAL INFORMATION AND INSTRUCTIONS
------------------------------------

`thesis.tex' is the main file. It calls all other files.

Create your own thesis by editing/creating the following LaTeX files:

          thesis.tex      
              This is where you comment in or out the various
              optional sections. You also put in your title,
              name, committee members, etc.
        * preface.tex
        * acknowl.tex
        * abstract.tex
          chapter1.tex
            .
            :
          chapter-.tex
          ref.tex or refs.bib
              If you use BibTeX
        * glossary.tex
        * appendixA.tex
            .
            :
        * appendix-.tex
          vitae.tex

NOTE: All files preceded by an asterisk (`*') are optional.  If any don't
exist they will be ignored during compilation.  The table of contents,
list of tables and list of figures are all generated automatically.

You probably need to edit `thesis.tex' for the correct number of chapters, etc.
As you create new files, be sure to uncomment the `\include{file.tex}' for the
appropriate file.  If there are files that you don't use, comment the same
include line.  Comment `\include{listoffigures}' and `\include{listoftables}'
as necessary depending on whether you have figures or tables in your thesis.
You comment by inserting a % at the beginning of the line.

The simplest way to insert graphics is to create a .pdf file and
include it as in the example in chapter 7. If you do this, you need
to invoke pdflatex instead of just latex. Invoking pdflatex produces
a .pdf file output. Invoking latex produces a .dvi output which can
be viewed with the appropriate viewer. Another way to include
graphics is to use `xfig' or any other package that produces .ps
or .eps files. 

IMPORTANT: when printing the .pdf file from acrobat reader, in the
Page Handling box, set Page scaling to "None" and uncheck the
Auto-Rotate and the Choose paper boxes.

To create a typeset document, you need to invoke LaTeX and/or BibTeX.

If you don't use BibTeX and hardcode all your references, you need
to invoke latex (or pdflatex) twice to get the references and table
of contents right.

If you use BibTeX, you need to invoke the programs in the following sequence:
latex, bibtex, latex, latex (or pdflatex, bibtex, pdflatex, pdflatex).
The first invocation generates the references for bibtex. Then
bibtex extracts data from the database. The next two invocations
of latex set the references right.  If you don't add new citations
and don't modify the database, then only the two latex invocations
are necessary.


===============================================================================

SPELL CHECKING LaTeX FILES
--------------------------
You can check the spelling of your LaTeX files on UNIX by typing
`ispell -t file.tex'. You can install ispell on your PC as well
as described at the installation link mentioned above. I have
not tried myself.  The ispell program knows about LaTeX commands
and only checks the actual text in the file.


===============================================================================

REFERENCES ON LaTeX
-------------------
1.  L. Lamport. LaTeX: A Document Preparation System. 2nd Ed.
Addison-Wesley, 1994.

2.  M. Goosen, F. Mittelbach and A. Samarin. The LaTeX companion.
Addison-Wesley, 1994.

3.  Guide to LaTeX, 4th Edition.  Helmut Kopka & Patrick W. Daly.
Addison-Wesley, 2004.
===============================================================================

LIST OF FILES
-------------

(The current thesis.tex uses all files with a *)

  .emacs                  if you need it for WinEmacs
* abstract.tex            LaTeX file containing Abstract text
* acknowl.tex             LaTeX file containing Acknowledgements text
  appendixA.tex           LaTeX file containing an example Appendix A text
                             (modify thesis.tex to include)
* chapter1.tex            LaTeX file containing Introductory Chapter text
* chapter2.tex            LaTeX file containing Chapter 2 text
* chapter3.tex            LaTeX file containing Chapter 3 text
* chapter4.tex            LaTeX file containing Chapter 4 text
* chapter5.tex            LaTeX file containing Chapter 5 text
* chapter6.tex            LaTeX file containing Chapter 6 text
  chapter7.tex            LaTeX file containing an extra chapter with
                             examples of tables, figure and .pdf graphics
                             (modify thesis.tex to include)
  README                  this file
* ref.tex                 LaTeX file containing the references
  refs.bib                BibTeX file containing the references
                             (modify thesis.tex to use BibTex instead of
                              using refs.tex)
* utepcsthesis.sty        LaTeX package used for formatting the thesis
  thesis.tex              LaTeX main file, where you put your title,
                             committee members, etc. It has commands to
                             include all thesis parts
* vitae.tex               LaTeX file containing Curriculum Vitae text

===============================================================================

Bug reports to longpre@utep.edu
