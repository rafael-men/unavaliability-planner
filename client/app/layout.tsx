import type { Metadata } from "next";
import "./globals.css";
import "primereact/resources/themes/lara-dark-blue/theme.css";
import "primereact/resources/primereact.min.css";
import "primeicons/primeicons.css";
import { Providers } from "./providers";
import { Footer } from "./components/Footer";

export const metadata: Metadata = {
  title: "Indisponibilidade",
  description: "Sistema de controle de indisponibilidade",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR" data-theme="dark" className="dark">
      <head>
         <link rel="preconnect" href="https://fonts.googleapis.com"/>
         <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous"/>
         <link href="https://fonts.googleapis.com/css2?family=Roboto+Slab:wght@100..900&display=swap" rel="stylesheet"/>
         <link href="/icon.png" rel="icon" />
      </head>
      <body className="min-h-screen bg-background text-foreground antialiased flex flex-col">
        <Providers>
          <div className="flex-1 flex flex-col">{children}</div>
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
