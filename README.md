# Key_Lock - Secure File Sharing System
# Overview

KeyLock is a secure file sharing system built using Spring Boot and Spring Security, designed to ensure safe file sharing using RSA encryption.

Each user is assigned a unique RSA private key during registration, which is required later to securely access and download files.

# Features
  Secure Authentication & Authorization

      Role-based access: Admin and Employee
  
      Spring Security integration
  
  RSA Key-Based Security

      Unique private key generated at registration
   
      Key shown only once (one-time access)
   
      Required for secure file download
   
  File Management

       Upload and manage files (Admin)
       Secure download access (Employee)
  Data Protection
  
       Passwords encrypted using BCrypt
       Secure session handling
       One-time key visibility
       
  User-Friendly UI

       Built with Thymeleaf
       Clean and responsive design
       
# Tech Stack

      Backend: Spring Boot, Spring Security
      Frontend: Thymeleaf, HTML, CSS
      Database: MySQL
      Build Tool: Maven
