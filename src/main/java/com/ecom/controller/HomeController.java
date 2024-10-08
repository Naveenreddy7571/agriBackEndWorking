package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@CrossOrigin
public class HomeController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private CartService cartService;

    @ModelAttribute
    public void getUserDetails(Principal p, Model m) {
        if (p != null) {
            String email = p.getName();
            UserDtls userDtls = userService.getUserByEmail(email);
            m.addAttribute("user", userDtls);
            Integer countCart = cartService.getCountCart(userDtls.getId());
            m.addAttribute("countCart", countCart);
        }

        List<Category> allActiveCategory = categoryService.getAllActiveCategory();
        m.addAttribute("categorys", allActiveCategory);
    }
//
//    @GetMapping("/products")
//    public Page<Product> getAllProducts(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size) {
//
//
//        System.out.println("came to fertch");
//
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Product> productPage = productService.findAll(pageable);
//
//        if (productPage.hasContent()) {
//            return  ResponseEntity.ok(productPage);
//        } else {
//            return ResponseEntity.ok(new PageImpl<>(new ArrayList<>(), pageable, 0)); // Return empty page if no products found
//        }
//
////        Pageable pageable = PageRequest.of(page, size);
////        return productService.findAll(pageable);
//    }

//    @GetMapping("/products")
//    public ResponseEntity<Page<Product>> getAllProducts(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size) {
//
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Product> productPage = productService.findAll(pageable);
//        System.out.println(productPage);
//
//        if (productPage.hasContent()) {
//            return ResponseEntity.ok(productPage);
//        } else {
//            return ResponseEntity.ok(new PageImpl<>(new ArrayList<>(), pageable, 0)); // Return empty page if no products found
//        }
//    }

//    @GetMapping("/products")
//    public ResponseEntity<Page<Product>> getAllProducts(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size) {
//
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Product> productPage = productService.findAll(pageable);
//        System.out.println(productPage);
//
//        // Check if it's the last page and has no content
//        if (!productPage.hasContent() && productPage.getTotalPages() > 0) {
//            // If last page is reached and has no content, reset to the first page
//            pageable = PageRequest.of(0, size); // Fetch from the first page
//            productPage = productService.findAll(pageable);
//        }
//
//        if (productPage.hasContent()) {
//            return ResponseEntity.ok(productPage);
//        } else {
//            return ResponseEntity.ok(new PageImpl<>(new ArrayList<>(), pageable, 0)); // Return empty page if no products found
//        }
//    }
//


    @GetMapping("/products")
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) { // Default size changed to 8

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productService.findAll(pageable);
        System.out.println(productPage);

        // Check if the current page has less than the required number of products
        if (productPage.getContent().size() < size && productPage.getTotalPages() > 1) {
            // We need to fetch additional products
            int requiredProducts = size - productPage.getContent().size()-1;
            int additionalPage = 0; // Start from the second page
            List<Product> additionalProducts = new ArrayList<>(productPage.getContent());

            // Fetch products until we have enough
            while (additionalProducts.size() < size && additionalPage < productPage.getTotalPages()) {
                pageable = PageRequest.of(additionalPage, size);
                Page<Product> additionalProductPage = productService.findAll(pageable);
                additionalProducts.addAll(additionalProductPage.getContent());
                additionalPage++;
            }
            additionalProducts.remove(additionalProducts.get(additionalProducts.size()-1));

            // Create a new Page with the combined products
            productPage = new PageImpl<>(additionalProducts, pageable, productPage.getTotalElements());
        }

        // If the final page has no content
        if (productPage.getContent().isEmpty()) {
            return ResponseEntity.ok(new PageImpl<>(new ArrayList<>(), pageable, 0)); // Return empty page if no products found
        }

        return ResponseEntity.ok(productPage);
    }



    @GetMapping("/allproducts")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        System.out.println(products);

        // Convert to ProductDto to include the full image URL
        List<Product> productDtos = products.stream().map(product -> {
            Product productDto = new Product();
            productDto.setId(product.getId());
            productDto.setTitle(product.getTitle());
            productDto.setDescription(product.getDescription());
            productDto.setCategory(product.getCategory());
            productDto.setPrice(product.getPrice());
            productDto.setStock(product.getStock());
            productDto.setDiscount(product.getDiscount());
            productDto.setDiscountPrice(product.getDiscountPrice());
            productDto.setIsActive(product.getIsActive());

            // Full URL to the image
            String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/img/product_img/")
                    .path(product.getImage())
                    .toUriString();
            productDto.setImage(imageUrl);

            return productDto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(productDtos);
    }



    @GetMapping("/product/{id}")
    public ResponseEntity<Product> product(@PathVariable int id) {

        System.out.println("came to get the individuL PRODUCT");
        System.out.println(id);
        Product productById = productService.getProductById(id);
        //m.addAttribute("product", productById);
        return ResponseEntity.ok(productById);
    }

    @PostMapping("/saveUser")
    public ResponseEntity<String> saveUser(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file) throws IOException {
        Boolean existsEmail = userService.existsEmail(user.getEmail());

        if (existsEmail) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        } else {
            String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
            user.setProfileImage(imageName);
            UserDtls saveUser = userService.saveUser(user);

            if (!ObjectUtils.isEmpty(saveUser)) {
                if (!file.isEmpty()) {
                    File saveFile = new ClassPathResource("static/img").getFile();
                    Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator + file.getOriginalFilename());
                    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                }
                return ResponseEntity.status(HttpStatus.OK).body("Register successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong on the server");
            }
        }
    }


    @GetMapping("/search")
    public String searchProduct(@RequestParam String ch, Model m) {
        List<Product> searchProducts = productService.searchProduct(ch);
        m.addAttribute("products", searchProducts);
        List<Category> categories = categoryService.getAllActiveCategory();
        m.addAttribute("categories", categories);
        return "products";

    }

}
