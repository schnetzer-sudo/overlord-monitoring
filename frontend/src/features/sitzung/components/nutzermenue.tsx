"use client";

import Link from "next/link";
import { KeyRound, LogOut, UserRound } from "lucide-react";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { useTexte } from "@/i18n/provider";
import { ROUTEN } from "@/lib/routen";

import { useAbmelden } from "../hooks";
import type { Selbstauskunft } from "../api";

/**
 * Nutzermenü mit Abmelden und dem freiwilligen Weg zur Passwortänderung.
 *
 * Die Rollenbezeichnung ist Anzeige, keine Berechtigung — was ein Nutzer darf,
 * entscheidet ausschließlich das Backend.
 */
export function Nutzermenue({ auskunft }: { auskunft: Selbstauskunft }) {
  const texte = useTexte();
  const abmelden = useAbmelden();

  const rollen: Record<string, string> = texte.rolle;
  const rollentext = rollen[auskunft.role] ?? auskunft.role;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          className="min-h-bedienelement h-auto shrink-0 px-2"
          aria-label={texte.nutzermenue.oeffnen}
        >
          <UserRound aria-hidden="true" />
          <span className="hidden max-w-32 truncate md:inline">{auskunft.username}</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-60">
        <DropdownMenuLabel className="font-normal">
          <span className="text-muted-foreground text-beiwerk block">
            {texte.nutzermenue.angemeldetAls}
          </span>
          <span className="block truncate font-medium">{auskunft.username}</span>
          <span className="text-muted-foreground text-beiwerk block">{rollentext}</span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild className="min-h-bedienelement">
          <Link href={ROUTEN.passwort}>
            <KeyRound aria-hidden="true" />
            {texte.nutzermenue.passwortAendern}
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem
          className="min-h-bedienelement"
          disabled={abmelden.isPending}
          onSelect={() => abmelden.mutate()}
        >
          <LogOut aria-hidden="true" />
          {abmelden.isPending ? texte.nutzermenue.abmeldenLaeuft : texte.nutzermenue.abmelden}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
