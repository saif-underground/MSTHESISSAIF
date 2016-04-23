;;;;;;;;;;;;;;;;;;;;;;;;;;; -*- Mode: Emacs-Lisp -*- ;;;;;;;;;;;;;;;;;;;;;;;;;;
;; .emacs --- A simple startup file, enabling Auctex (and ispell.info)
;; Author          : Claus Dethlefsen
;; Created On      : Wed Aug 30 10:41:44 2000
;; Last Modified By: Claus Dethlefsen
;; Last Modified On: Thu Jan 10 12:44:02 2002
;; Update Count    : 8
;; Status          : Ok, but adjust the path to auctex
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Put this file in your HOME directory  (e.g. c:\)
;; See http://www.math.auc.dk/~dethlef/Tips/
;; for more information on setting up LaTeX, Emacs, AucTeX etc.

;; -----------------------------------------------------------------
;; AUC TeX

;(add-to-list 'load-path "c:/program files/emacs/site-lisp/auctex/")
(add-to-list 'Info-default-directory-list "c:/program files/emacs/site-lisp/auctex/doc/")
(load "tex-site") 

;; -----------------------------------------------------------------
;; Reftex activation (Reftex is included with Emacs 21.1)

(autoload 'reftex-mode     "reftex" "RefTeX Minor Mode" t)
(autoload 'turn-on-reftex  "reftex" "RefTeX Minor Mode" nil)
(autoload 'reftex-citation "reftex-cite" "Make citation" nil)
(autoload 'reftex-index-phrase-mode "reftex-index" "Phrase mode" t)
(add-hook 'LaTeX-mode-hook 'turn-on-reftex)   ; with AUCTeX LaTeX mode
(add-hook 'latex-mode-hook 'turn-on-reftex)   ; with Emacs latex mode

;; ----------------------------------------------------------------------
;; Info for Ispell
(add-to-list 'Info-default-directory-list "c:/usr/local/info/")      

;; end of .emacs
